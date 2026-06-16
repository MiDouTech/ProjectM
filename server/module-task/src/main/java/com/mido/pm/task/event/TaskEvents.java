package com.mido.pm.task.event;

/** 任务域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class TaskEvents {

    public static final String CREATED = "task.created";
    public static final String ASSIGNED = "task.assigned";
    public static final String STATUS_CHANGED = "task.status.changed";

    private TaskEvents() {
    }
}
