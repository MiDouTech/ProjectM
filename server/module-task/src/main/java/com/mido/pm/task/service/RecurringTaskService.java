package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.task.domain.TaskRecurrence;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.event.TaskEvents;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 循环任务实例生成（策略 A·急切定额）。
 *
 * <p>以带 recur_rule 的任务为模板，按规则向后生成若干子任务实例（parent_id 指向模板）。每个实例独立
 * 计状态/指派/工时；实例自身不再循环（recur_rule 置空）。单次最多生成 {@link #MAX_GENERATE} 个，并受
 * 规则 count/until 约束。按出现日期幂等去重，故可重复调用做「补齐下一批」。</p>
 */
@Service
public class RecurringTaskService {

    /** 单次生成实例数上限（策略 A 急切定额；日后可挂定时任务升级为滚动窗口）。 */
    static final int MAX_GENERATE = 12;

    private final PmTaskMapper taskMapper;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public RecurringTaskService(PmTaskMapper taskMapper, DomainEventPublisher eventPublisher,
                                AuditLogService auditLogService) {
        this.taskMapper = taskMapper;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    /**
     * 为模板任务生成后续实例，返回本次新建数量。无循环规则或无锚定日期则不生成（返回 0）。
     * 已存在的出现日期跳过（幂等），故重复调用安全。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(Long templateId) {
        PmTask tpl = taskMapper.selectById(templateId);
        if (tpl == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        TaskRecurrence rec = TaskRecurrence.parse(tpl.getRecurRule());
        if (rec == null) {
            return 0;
        }
        // 锚定日期：优先 start_date，缺则 due_date；两者皆无无法定位出现日，跳过
        boolean anchorOnStart = tpl.getStartDate() != null;
        LocalDate anchor = anchorOnStart ? tpl.getStartDate() : tpl.getDueDate();
        if (anchor == null) {
            return 0;
        }

        Set<LocalDate> existing = existingOccurrenceDates(templateId);
        int limit = rec.count() != null ? Math.min(MAX_GENERATE, rec.count() - 1) : MAX_GENERATE;
        int generated = 0;
        for (int i = 1; i <= limit && generated < MAX_GENERATE; i++) {
            LocalDate start = tpl.getStartDate() == null ? null : rec.shift(tpl.getStartDate(), i);
            LocalDate due = tpl.getDueDate() == null ? null : rec.shift(tpl.getDueDate(), i);
            LocalDate occDate = anchorOnStart ? start : due;
            if (rec.until() != null && occDate.isAfter(rec.until())) {
                break;
            }
            if (existing.contains(occDate)) {
                continue;
            }
            insertInstance(tpl, start, due);
            generated++;
        }
        return generated;
    }

    /** 已生成实例的出现日期集合（按锚定字段，优先 start_date），用于幂等去重。 */
    private Set<LocalDate> existingOccurrenceDates(Long templateId) {
        List<PmTask> children = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getParentId, templateId));
        Set<LocalDate> dates = new HashSet<>();
        for (PmTask c : children) {
            LocalDate d = c.getStartDate() != null ? c.getStartDate() : c.getDueDate();
            if (d != null) {
                dates.add(d);
            }
        }
        return dates;
    }

    private void insertInstance(PmTask tpl, LocalDate start, LocalDate due) {
        PmTask inst = new PmTask();
        inst.setProjectId(tpl.getProjectId());
        inst.setDeptId(tpl.getDeptId());
        inst.setParentId(tpl.getId());
        inst.setTitle(tpl.getTitle());
        inst.setDescription(tpl.getDescription());
        inst.setAssigneeId(tpl.getAssigneeId());
        inst.setStatus(TaskStatus.NOT_STARTED.getCode());
        inst.setPriority(tpl.getPriority());
        inst.setStage(tpl.getStage());
        inst.setStartDate(start);
        inst.setDueDate(due);
        inst.setIsMilestone(tpl.getIsMilestone());
        inst.setRecurRule(null); // 实例自身不再循环
        taskMapper.insert(inst);

        eventPublisher.publish(TaskEvents.CREATED, payload(
                "taskId", inst.getId(), "projectId", inst.getProjectId(), "title", inst.getTitle()));
        if (inst.getAssigneeId() != null) {
            eventPublisher.publish(TaskEvents.ASSIGNED, payload(
                    "taskId", inst.getId(), "assigneeId", inst.getAssigneeId()));
        }
        auditLogService.record(AuditActions.TARGET_TASK, inst.getId(), AuditActions.CREATED, null);
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }
}
