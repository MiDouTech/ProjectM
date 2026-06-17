package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.domain.TaskWorkflow;
import com.mido.pm.task.dto.KanbanColumnVO;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.dto.TaskQueryDTO;
import com.mido.pm.task.dto.TaskUpdateDTO;
import com.mido.pm.task.dto.TaskVO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.entity.PmTaskDependency;
import com.mido.pm.task.event.TaskEvents;
import com.mido.pm.task.mapper.PmTaskDependencyMapper;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 任务服务（P0）：CRUD、子任务、指派、状态流转（默认工作流）、看板分组、列表筛选。
 * 写操作同事务发 Outbox 事件。依赖/工时/循环规则留 P1。
 */
@Service
public class TaskService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final PmTaskMapper taskMapper;
    private final PmTaskDependencyMapper dependencyMapper;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public TaskService(PmTaskMapper taskMapper, PmTaskDependencyMapper dependencyMapper,
                       DomainEventPublisher eventPublisher, AuditLogService auditLogService) {
        this.taskMapper = taskMapper;
        this.dependencyMapper = dependencyMapper;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    /**
     * 改期依赖约束（FS）：本任务开始不得早于任一前置的完成日；任一后继的开始不得早于本任务完成日。
     * 仅在相关日期均存在时校验；冲突抛 409。
     */
    private void validateSchedule(Long taskId, LocalDate start, LocalDate due) {
        if (start == null && due == null) {
            return;
        }
        if (start != null) {
            List<Long> predIds = dependencyMapper.selectList(Wrappers.<PmTaskDependency>lambdaQuery()
                            .eq(PmTaskDependency::getSuccessorId, taskId))
                    .stream().map(PmTaskDependency::getPredecessorId).toList();
            for (PmTask pred : loadTasks(predIds)) {
                if (pred.getDueDate() != null && start.isBefore(pred.getDueDate())) {
                    throw new BizException(ErrorCode.CONFLICT,
                            "开始日不能早于前置任务「" + pred.getTitle() + "」完成日 " + pred.getDueDate());
                }
            }
        }
        if (due != null) {
            List<Long> succIds = dependencyMapper.selectList(Wrappers.<PmTaskDependency>lambdaQuery()
                            .eq(PmTaskDependency::getPredecessorId, taskId))
                    .stream().map(PmTaskDependency::getSuccessorId).toList();
            for (PmTask succ : loadTasks(succIds)) {
                if (succ.getStartDate() != null && succ.getStartDate().isBefore(due)) {
                    throw new BizException(ErrorCode.CONFLICT,
                            "完成日晚于后继任务「" + succ.getTitle() + "」开始日 " + succ.getStartDate());
                }
            }
        }
    }

    private List<PmTask> loadTasks(List<Long> ids) {
        return ids.isEmpty() ? List.of() : taskMapper.selectBatchIds(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskCreateDTO dto) {
        PmTask task = new PmTask();
        task.setProjectId(dto.projectId());
        task.setParentId(dto.parentId() == null ? 0L : dto.parentId());
        task.setTitle(dto.title());
        task.setDescription(dto.description());
        task.setAssigneeId(dto.assigneeId());
        task.setStatus(TaskStatus.NOT_STARTED.getCode());
        task.setPriority(dto.priority());
        task.setStage(dto.stage());
        task.setStartDate(dto.startDate());
        task.setDueDate(dto.dueDate());
        task.setIsMilestone(dto.isMilestone() == null ? 0 : dto.isMilestone());
        taskMapper.insert(task);

        eventPublisher.publish(TaskEvents.CREATED, payload(
                "taskId", task.getId(), "projectId", task.getProjectId(), "title", task.getTitle()));
        if (task.getAssigneeId() != null) {
            eventPublisher.publish(TaskEvents.ASSIGNED, payload(
                    "taskId", task.getId(), "assigneeId", task.getAssigneeId()));
        }
        auditLogService.record(AuditActions.TARGET_TASK, task.getId(), AuditActions.CREATED, null);
        return task.getId();
    }

    public TaskVO get(Long id) {
        return toVO(requireExists(id));
    }

    public List<TaskVO> subtasks(Long parentId) {
        return taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                        .eq(PmTask::getParentId, parentId).orderByAsc(PmTask::getId))
                .stream().map(this::toVO).toList();
    }

    public PageResult<TaskVO> page(TaskQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);
        Page<PmTask> page = new Page<>(pageNo, size);
        LambdaQueryWrapper<PmTask> wrapper = buildQuery(query);
        Page<PmTask> result = taskMapper.selectPage(page, wrapper);
        List<TaskVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TaskUpdateDTO dto) {
        PmTask task = requireExists(id);
        // 改期（含甘特拖拽）须满足 FS 依赖约束：后置不能早于前置完成
        validateSchedule(id, dto.startDate(), dto.dueDate());
        // 编辑前后字段差异 → 活动流（一次写一条，含 changes 列表）
        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "title", task.getTitle(), dto.title());
        addChange(changes, "priority", task.getPriority(), dto.priority());
        addChange(changes, "stage", task.getStage(), dto.stage());
        addChange(changes, "startDate", task.getStartDate(), dto.startDate());
        addChange(changes, "dueDate", task.getDueDate(), dto.dueDate());
        if (dto.isMilestone() != null) {
            addChange(changes, "isMilestone", task.getIsMilestone(), dto.isMilestone());
        }
        addChange(changes, "description", task.getDescription(), dto.description());

        task.setTitle(dto.title());
        task.setPriority(dto.priority());
        task.setStage(dto.stage());
        task.setStartDate(dto.startDate());
        task.setDueDate(dto.dueDate());
        if (dto.isMilestone() != null) {
            task.setIsMilestone(dto.isMilestone());
        }
        task.setDescription(dto.description());
        taskMapper.updateById(task);

        if (!changes.isEmpty()) {
            auditLogService.record(AuditActions.TARGET_TASK, id, AuditActions.UPDATED,
                    Map.of("changes", changes));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PmTask task = requireExists(id);
        taskMapper.deleteById(id); // 逻辑删除（@TableLogic）
        eventPublisher.publish(TaskEvents.DELETED, payload(
                "taskId", id, "projectId", task.getProjectId()));
    }

    /** 批量改状态：逐条复用 changeStatus（各自校验工作流合法性 + 发事件 + 记活动）；任一非法整批回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public void batchChangeStatus(List<Long> ids, String targetStatus) {
        requireNonEmpty(ids);
        for (Long id : ids) {
            changeStatus(id, targetStatus);
        }
    }

    /** 批量改负责人：逐条复用 assign（各自发 task.assigned + 记活动）。 */
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(List<Long> ids, Long assigneeId) {
        requireNonEmpty(ids);
        for (Long id : ids) {
            assign(id, assigneeId);
        }
    }

    /** 批量删除（逻辑删）：逐条复用 delete（各自发 task.deleted）。 */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        requireNonEmpty(ids);
        for (Long id : ids) {
            delete(id);
        }
    }

    private void requireNonEmpty(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请至少选择一条任务");
        }
    }

    /** 指派/改派，发 task.assigned。 */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long assigneeId) {
        PmTask task = requireExists(id);
        Long oldAssignee = task.getAssigneeId();
        task.setAssigneeId(assigneeId);
        taskMapper.updateById(task);
        eventPublisher.publish(TaskEvents.ASSIGNED, payload("taskId", id, "assigneeId", assigneeId));
        auditLogService.record(AuditActions.TARGET_TASK, id, AuditActions.ASSIGNED,
                fromTo(oldAssignee, assigneeId));
    }

    /** 状态流转（看板拖拽亦走此）：校验默认工作流合法流转，发 task.status.changed。 */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String targetStatus) {
        PmTask task = requireExists(id);
        TaskStatus from = TaskStatus.fromCode(task.getStatus());
        TaskStatus to = TaskStatus.fromCode(targetStatus);
        if (to == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法目标状态: " + targetStatus);
        }
        TaskWorkflow.assertTransit(from, to);
        task.setStatus(to.getCode());
        taskMapper.updateById(task);
        eventPublisher.publish(TaskEvents.STATUS_CHANGED, payload(
                "taskId", id, "from", from == null ? null : from.getCode(), "to", to.getCode()));
        auditLogService.record(AuditActions.TARGET_TASK, id, AuditActions.STATUS_CHANGED,
                fromTo(from == null ? null : from.getCode(), to.getCode()));
    }

    /** 看板：按工作流状态顺序分组返回某项目任务。 */
    public List<KanbanColumnVO> kanban(Long projectId) {
        List<PmTask> tasks = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getProjectId, projectId).orderByDesc(PmTask::getId));
        List<KanbanColumnVO> columns = new java.util.ArrayList<>();
        for (TaskStatus s : TaskStatus.values()) {
            List<TaskVO> cards = tasks.stream()
                    .filter(t -> s.getCode().equals(t.getStatus()))
                    .map(this::toVO).toList();
            columns.add(new KanbanColumnVO(s.getCode(), cards));
        }
        return columns;
    }

    // ===== 内部 =====

    private LambdaQueryWrapper<PmTask> buildQuery(TaskQueryDTO q) {
        LambdaQueryWrapper<PmTask> w = Wrappers.<PmTask>lambdaQuery()
                .eq(q.projectId() != null, PmTask::getProjectId, q.projectId())
                .eq(q.assigneeId() != null, PmTask::getAssigneeId, q.assigneeId())
                .eq(q.status() != null && !q.status().isBlank(), PmTask::getStatus, q.status());
        if (Boolean.TRUE.equals(q.overdue())) {
            // “今天”取统一服务器时区（启动时 TimeZone.setDefault，默认 Asia/Shanghai），跨环境一致。
            w.lt(PmTask::getDueDate, LocalDate.now())
                    .notIn(PmTask::getStatus, TaskStatus.DONE.getCode(), TaskStatus.ACCEPTED.getCode());
        }
        applySort(w, q.sort());
        return w;
    }

    /** sort=field,asc|desc，字段白名单防注入；缺省按 id 降序。 */
    private void applySort(LambdaQueryWrapper<PmTask> w, String sort) {
        if (sort == null || sort.isBlank()) {
            w.orderByDesc(PmTask::getId);
            return;
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean asc = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1].trim());
        switch (field) {
            case "priority" -> w.orderBy(true, asc, PmTask::getPriority);
            case "dueDate" -> w.orderBy(true, asc, PmTask::getDueDate);
            case "startDate" -> w.orderBy(true, asc, PmTask::getStartDate);
            case "createTime" -> w.orderBy(true, asc, PmTask::getCreateTime);
            default -> w.orderByDesc(PmTask::getId);
        }
    }

    /** 记一个字段变更（值不等才记；from/to 允许 null，故不用 Map.of）。 */
    private void addChange(List<Map<String, Object>> changes, String field, Object oldVal, Object newVal) {
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("from", oldVal);
        change.put("to", newVal);
        changes.add(change);
    }

    /** 构造 {from,to} 明细（值允许 null）。 */
    private Map<String, Object> fromTo(Object from, Object to) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", from);
        detail.put("to", to);
        return detail;
    }

    private PmTask requireExists(Long id) {
        PmTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }

    private TaskVO toVO(PmTask t) {
        return new TaskVO(t.getId(), t.getProjectId(), t.getParentId(), t.getTitle(), t.getDescription(),
                t.getAssigneeId(), t.getStatus(), t.getPriority(), t.getStage(),
                t.getStartDate(), t.getDueDate(), t.getIsMilestone(), t.getCreateTime());
    }
}
