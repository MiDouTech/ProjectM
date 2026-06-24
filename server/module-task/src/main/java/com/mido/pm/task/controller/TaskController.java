package com.mido.pm.task.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.common.audit.ActivityVO;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.task.dto.KanbanColumnVO;
import com.mido.pm.task.dto.TaskAssignDTO;
import com.mido.pm.task.dto.TaskBatchAssignDTO;
import com.mido.pm.task.dto.TaskBatchDeleteDTO;
import com.mido.pm.task.dto.TaskBatchStatusDTO;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.dto.TaskQueryDTO;
import com.mido.pm.task.dto.TaskTransitionDTO;
import com.mido.pm.task.dto.TaskUpdateDTO;
import com.mido.pm.task.dto.TaskVO;
import com.mido.pm.task.service.RecurringTaskService;
import com.mido.pm.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 任务 CRUD / 子任务 / 指派 / 状态流转(看板拖拽) / 列表 / 看板。 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final RecurringTaskService recurringTaskService;
    private final AuditLogService auditLogService;

    public TaskController(TaskService taskService, RecurringTaskService recurringTaskService,
                          AuditLogService auditLogService) {
        this.taskService = taskService;
        this.recurringTaskService = recurringTaskService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody TaskCreateDTO dto) {
        return R.ok(taskService.create(dto));
    }

    @GetMapping("/{id}")
    public R<TaskVO> get(@PathVariable Long id) {
        return R.ok(taskService.get(id));
    }

    @GetMapping("/{id}/subtasks")
    public R<List<TaskVO>> subtasks(@PathVariable Long id) {
        return R.ok(taskService.subtasks(id));
    }

    @PostMapping("/query")
    public R<PageResult<TaskVO>> query(@RequestBody TaskQueryDTO query) {
        return R.ok(taskService.page(query));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateDTO dto) {
        taskService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    /** 循环任务：按 recur_rule 补齐后续实例（幂等，可重复调用补齐下一批），返回本次新建数量。 */
    @PostMapping("/{id}/recurrence/generate")
    public R<Integer> generateRecurrence(@PathVariable Long id) {
        return R.ok(recurringTaskService.generate(id));
    }

    /** 指派/改派。 */
    @PutMapping("/{id}/assignee")
    public R<Void> assign(@PathVariable Long id, @RequestBody TaskAssignDTO dto) {
        taskService.assign(id, dto.assigneeId());
        return R.ok();
    }

    /** 状态流转 / 看板拖拽改状态（校验工作流合法流转）。 */
    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @Valid @RequestBody TaskTransitionDTO dto) {
        taskService.changeStatus(id, dto.targetStatus());
        return R.ok();
    }

    /** 看板：按状态分组返回项目任务。 */
    @GetMapping("/kanban")
    public R<List<KanbanColumnVO>> kanban(@RequestParam Long projectId) {
        return R.ok(taskService.kanban(projectId));
    }

    /** 日历叠加：当前用户可见、截止日落在 [from,to] 的任务(含里程碑)，供日历视图叠加显示。 */
    @GetMapping("/calendar")
    public R<List<com.mido.pm.task.dto.CalendarTaskVO>> calendarTasks(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return R.ok(taskService.calendarTasks(from, to));
    }

    /** 批量改状态（逐条校验工作流合法性，任一非法整批回滚，每条各自发 task.status.changed）。 */
    @PostMapping("/batch/transition")
    public R<Void> batchTransition(@Valid @RequestBody TaskBatchStatusDTO dto) {
        taskService.batchChangeStatus(dto.ids(), dto.targetStatus());
        return R.ok();
    }

    /** 批量改负责人（每条各自发 task.assigned）。 */
    @PostMapping("/batch/assignee")
    public R<Void> batchAssign(@Valid @RequestBody TaskBatchAssignDTO dto) {
        taskService.batchAssign(dto.ids(), dto.assigneeId());
        return R.ok();
    }

    /** 批量删除（逻辑删，每条各自发 task.deleted）。 */
    @PostMapping("/batch/delete")
    public R<Void> batchDelete(@Valid @RequestBody TaskBatchDeleteDTO dto) {
        taskService.batchDelete(dto.ids());
        return R.ok();
    }

    /** 活动日志（谁在何时改了什么 X→Y）：分页倒序，page 从 1、size 默认 20 上限 100。 */
    @GetMapping("/{id}/activities")
    public R<PageResult<ActivityVO>> activities(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "20") Long size) {
        return R.ok(auditLogService.query(AuditActions.TARGET_TASK, id, page, size));
    }
}
