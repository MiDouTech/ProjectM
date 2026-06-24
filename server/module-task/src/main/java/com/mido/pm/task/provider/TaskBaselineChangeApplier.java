package com.mido.pm.task.provider;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 重大任务变更应用器：变更审批通过（或免审）后，把 after_payload 的起止日期/负责人覆盖回 pm_task。
 * 直接经 taskMapper 写表（不走 TaskService，避免与变更/审批编排成环）。
 * 不另发业务事件：回写写操作由 ChangeApplyService 同事务发 change.applied 覆盖（任务域无 task.updated 事件）。
 */
@Component
public class TaskBaselineChangeApplier implements ChangeApplier {

    private static final Logger log = LoggerFactory.getLogger(TaskBaselineChangeApplier.class);

    private final PmTaskMapper taskMapper;

    public TaskBaselineChangeApplier(PmTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public boolean supports(String bizType) {
        return "task".equals(bizType);
    }

    @Override
    public void apply(PmChangeRequest request) {
        PmTask t = taskMapper.selectById(request.getBizId());
        if (t == null) {
            return;
        }
        JSONObject after = JSONUtil.parseObj(request.getAfterPayload() == null ? "{}" : request.getAfterPayload());
        boolean changed = false;
        for (String key : after.keySet()) {
            switch (key) {
                case "startDate" -> {
                    t.setStartDate(parseDate(after.getStr("startDate")));
                    changed = true;
                }
                case "dueDate" -> {
                    t.setDueDate(parseDate(after.getStr("dueDate")));
                    changed = true;
                }
                case "assigneeId" -> {
                    t.setAssigneeId(after.getLong("assigneeId"));
                    changed = true;
                }
                default -> log.warn("重大任务变更含未知字段，已忽略：changeId={}, field={}", request.getId(), key);
            }
        }
        if (changed) {
            taskMapper.updateById(t);
        }
    }

    private LocalDate parseDate(String iso) {
        return iso == null || iso.isBlank() ? null : LocalDate.parse(iso);
    }
}
