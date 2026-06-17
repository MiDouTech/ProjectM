package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.task.dto.PersonWorkHourSummaryVO;
import com.mido.pm.task.dto.WorkHourCreateDTO;
import com.mido.pm.task.dto.WorkHourSummaryVO;
import com.mido.pm.task.dto.WorkHourUpdateDTO;
import com.mido.pm.task.dto.WorkHourVO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.entity.PmWorkHour;
import com.mido.pm.task.event.TaskEvents;
import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.task.mapper.PmWorkHourMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工时服务：登记/修改预估(est)/实际(actual)工时，任务级/项目级/人员级汇总（口径见 {@link com.mido.pm.task.domain.WorkHourCalc}）。
 * 登记/修改同事务发 workhour.logged。
 */
@Service
public class WorkHourService {

    /** 工时类型 */
    public static final String KIND_EST = "est";
    public static final String KIND_ACTUAL = "actual";
    private static final Set<String> KINDS = Set.of(KIND_EST, KIND_ACTUAL);
    /** 工时类别（与前端字典一致） */
    private static final Set<String> CATEGORIES = Set.of("设计", "研发", "文档", "测试", "其他");

    private final PmWorkHourMapper workHourMapper;
    private final PmTaskMapper taskMapper;
    private final DomainEventPublisher eventPublisher;

    public WorkHourService(PmWorkHourMapper workHourMapper, PmTaskMapper taskMapper,
                           DomainEventPublisher eventPublisher) {
        this.workHourMapper = workHourMapper;
        this.taskMapper = taskMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long log(WorkHourCreateDTO dto) {
        if (!KINDS.contains(dto.kind())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法工时类型: " + dto.kind());
        }
        if (!CATEGORIES.contains(dto.category())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法工时类别: " + dto.category());
        }
        requireTask(dto.taskId());

        PmWorkHour w = new PmWorkHour();
        w.setTaskId(dto.taskId());
        w.setUserId(currentUserId());
        w.setKind(dto.kind());
        w.setCategory(dto.category());
        w.setWorkDate(dto.workDate());
        w.setHours(dto.hours());
        w.setRemark(dto.remark());
        w.setCreateBy(currentUserId());
        workHourMapper.insert(w);

        publishLogged(w, "logged");
        return w.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WorkHourUpdateDTO dto) {
        if (!CATEGORIES.contains(dto.category())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法工时类别: " + dto.category());
        }
        PmWorkHour w = requireExists(id);
        w.setCategory(dto.category());
        w.setWorkDate(dto.workDate());
        w.setHours(dto.hours());
        w.setRemark(dto.remark());
        workHourMapper.updateById(w);

        publishLogged(w, "updated");
    }

    /** 某任务的工时记录（按日期、id 倒序）。 */
    public List<WorkHourVO> list(Long taskId) {
        return workHourMapper.selectList(Wrappers.<PmWorkHour>lambdaQuery()
                        .eq(PmWorkHour::getTaskId, taskId)
                        .orderByDesc(PmWorkHour::getWorkDate).orderByDesc(PmWorkHour::getId))
                .stream().map(this::toVO).toList();
    }

    /** 任务级汇总（口径：含本任务及其全部后代子任务的工时）。 */
    public WorkHourSummaryVO taskSummary(Long taskId) {
        return summarize(workHourMapper.selectList(Wrappers.<PmWorkHour>lambdaQuery()
                .in(PmWorkHour::getTaskId, taskAndDescendantIds(taskId))));
    }

    /** 项目级汇总（汇总本项目全部任务的工时）。 */
    public WorkHourSummaryVO projectSummary(Long projectId) {
        List<Long> taskIds = projectTaskIds(projectId);
        if (taskIds.isEmpty()) {
            return WorkHourSummaryVO.of(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return summarize(workHourMapper.selectList(Wrappers.<PmWorkHour>lambdaQuery()
                .in(PmWorkHour::getTaskId, taskIds)));
    }

    /** 人员级汇总（项目内按人分组，按实际工时降序）。 */
    public List<PersonWorkHourSummaryVO> personSummary(Long projectId) {
        List<Long> taskIds = projectTaskIds(projectId);
        if (taskIds.isEmpty()) {
            return List.of();
        }
        List<PmWorkHour> rows = workHourMapper.selectList(Wrappers.<PmWorkHour>lambdaQuery()
                .in(PmWorkHour::getTaskId, taskIds));
        // 按 user 分组累计预估/实际（保序）
        Map<Long, BigDecimal[]> byUser = new LinkedHashMap<>();
        for (PmWorkHour w : rows) {
            BigDecimal[] sums = byUser.computeIfAbsent(w.getUserId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (KIND_EST.equals(w.getKind())) {
                sums[0] = sums[0].add(nz(w.getHours()));
            } else if (KIND_ACTUAL.equals(w.getKind())) {
                sums[1] = sums[1].add(nz(w.getHours()));
            }
        }
        return byUser.entrySet().stream()
                .map(e -> new PersonWorkHourSummaryVO(e.getKey(), e.getValue()[0], e.getValue()[1],
                        com.mido.pm.task.domain.WorkHourCalc.progressPercent(e.getValue()[0], e.getValue()[1]),
                        com.mido.pm.task.domain.WorkHourCalc.remaining(e.getValue()[0], e.getValue()[1])))
                .sorted((a, b) -> b.actualHours().compareTo(a.actualHours()))
                .toList();
    }

    // ===== 内部 =====

    private WorkHourSummaryVO summarize(List<PmWorkHour> rows) {
        BigDecimal est = sumByKind(rows, KIND_EST);
        BigDecimal actual = sumByKind(rows, KIND_ACTUAL);
        return WorkHourSummaryVO.of(est, actual);
    }

    private BigDecimal sumByKind(List<PmWorkHour> rows, String kind) {
        return rows.stream().filter(w -> kind.equals(w.getKind()))
                .map(w -> nz(w.getHours())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 本任务 + 全部后代子任务 id（按 parent_id 逐层向下，防自环）。 */
    private List<Long> taskAndDescendantIds(Long rootId) {
        List<Long> all = new ArrayList<>();
        all.add(rootId);
        List<Long> frontier = List.of(rootId);
        while (!frontier.isEmpty()) {
            List<Long> children = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                            .in(PmTask::getParentId, frontier))
                    .stream().map(PmTask::getId).filter(id -> !all.contains(id)).toList();
            if (children.isEmpty()) {
                break;
            }
            all.addAll(children);
            frontier = children;
        }
        return all;
    }

    private List<Long> projectTaskIds(Long projectId) {
        return taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                        .eq(PmTask::getProjectId, projectId))
                .stream().map(PmTask::getId).toList();
    }

    private void publishLogged(PmWorkHour w, String action) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workHourId", w.getId());
        payload.put("taskId", w.getTaskId());
        payload.put("userId", w.getUserId());
        payload.put("kind", w.getKind());
        payload.put("category", w.getCategory());
        payload.put("hours", w.getHours());
        payload.put("action", action);
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(TaskEvents.WORKHOUR_LOGGED, payload);
    }

    private void requireTask(Long taskId) {
        if (taskMapper.selectById(taskId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
    }

    private PmWorkHour requireExists(Long id) {
        PmWorkHour w = workHourMapper.selectById(id);
        if (w == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工时记录不存在");
        }
        return w;
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private WorkHourVO toVO(PmWorkHour w) {
        return new WorkHourVO(w.getId(), w.getTaskId(), w.getUserId(), w.getKind(), w.getCategory(),
                w.getWorkDate(), w.getHours(), w.getRemark(), w.getCreateTime());
    }
}
