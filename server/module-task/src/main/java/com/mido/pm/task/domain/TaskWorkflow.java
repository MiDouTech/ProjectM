package com.mido.pm.task.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.mido.pm.task.domain.TaskStatus.ACCEPTED;
import static com.mido.pm.task.domain.TaskStatus.DONE;
import static com.mido.pm.task.domain.TaskStatus.IN_PROGRESS;
import static com.mido.pm.task.domain.TaskStatus.NOT_STARTED;

/**
 * 任务默认工作流：合法状态流转表（看板拖拽据此校验）。pm_workflow 可配版本留 P1。
 * 纯逻辑、可单测。
 */
public final class TaskWorkflow {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
            NOT_STARTED, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(NOT_STARTED, DONE),
            DONE, EnumSet.of(IN_PROGRESS, ACCEPTED),   // 已完成→打回进行中 / 验收
            ACCEPTED, EnumSet.of(DONE)                 // 验收不通过打回
    );

    private TaskWorkflow() {
    }

    public static boolean canTransit(TaskStatus from, TaskStatus to) {
        return from != null && to != null
                && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransit(TaskStatus from, TaskStatus to) {
        if (!canTransit(from, to)) {
            String f = from == null ? "?" : from.getCode();
            String t = to == null ? "?" : to.getCode();
            throw new BizException(ErrorCode.CONFLICT, "非法任务状态流转：" + f + " → " + t);
        }
    }
}
