package com.mido.pm.task.service;

import cn.hutool.json.JSONUtil;
import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.dto.TaskChangeRequestDTO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 重大任务（基线）变更发起：组装 before/after 快照后提交变更中心（{@link ChangeService}，复用审批引擎）。
 * 对标 Worktile「重大任务变更」。任务域负责字段快照与拟改值组装，变更域不耦合任务字段。
 * 日期以 ISO 字符串入快照（与 TaskBaselineChangeApplier 同源解析）。
 */
@Service
public class TaskChangeService {

    /** 被改实体域标识（变更台账 biz_type / ChangeApplier.supports）。 */
    public static final String BIZ_TYPE = "task";

    private static final Set<String> CHANGE_TYPES = Set.of("task_baseline");

    /** 可变更基线字段：现值取值器(快照) + 拟改值取值器(DTO)。日期为 ISO 字符串，负责人为用户 id。 */
    private record Field(String key, Function<PmTask, Object> current,
                         Function<TaskChangeRequestDTO, Object> proposed) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("startDate", t -> iso(t.getStartDate()), r -> iso(r.startDate())),
            new Field("dueDate", t -> iso(t.getDueDate()), r -> iso(r.dueDate())),
            new Field("assigneeId", PmTask::getAssigneeId, TaskChangeRequestDTO::assigneeId));

    private final PmTaskMapper taskMapper;
    private final ChangeService changeService;

    public TaskChangeService(PmTaskMapper taskMapper, ChangeService changeService) {
        this.taskMapper = taskMapper;
        this.changeService = changeService;
    }

    /** 发起重大任务变更：组装 before/after 后提交变更中心，返回变更单 id。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long taskId, TaskChangeRequestDTO req) {
        if (!CHANGE_TYPES.contains(req.changeType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法变更类型: " + req.changeType());
        }
        PmTask t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        // 起止顺序守卫：以拟改值覆盖现值后的有效区间，截止不得早于开始
        LocalDate effStart = req.startDate() != null ? req.startDate() : t.getStartDate();
        LocalDate effDue = req.dueDate() != null ? req.dueDate() : t.getDueDate();
        if (effStart != null && effDue != null && effDue.isBefore(effStart)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "截止日期不得早于开始日期");
        }
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (Field f : FIELDS) {
            Object cur = f.current().apply(t);
            before.put(f.key(), cur);
            Object proposed = f.proposed().apply(req);
            if (proposed != null && !proposed.equals(cur)) {
                after.put(f.key(), proposed);
            }
        }
        if (after.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "拟改值与现值一致，无变更内容");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("changeType", req.changeType());
        formData.put("taskId", taskId);
        formData.put("taskTitle", t.getTitle());

        ChangeSubmitCmd cmd = new ChangeSubmitCmd(BIZ_TYPE, taskId, req.changeType(),
                "重大任务变更·" + t.getTitle(), req.reason(), req.impact(),
                JSONUtil.toJsonStr(before), JSONUtil.toJsonStr(after), formData);
        return changeService.submit(cmd);
    }

    /** 某任务的变更历史（变更中心·任务视角）。 */
    public List<ChangeRequestVO> list(Long taskId) {
        return changeService.list(BIZ_TYPE, taskId, null);
    }

    private static Object iso(LocalDate d) {
        return d == null ? null : d.toString();
    }
}
