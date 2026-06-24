package com.mido.pm.task.event;

/** 任务域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class TaskEvents {

    public static final String CREATED = "task.created";
    public static final String ASSIGNED = "task.assigned";
    public static final String STATUS_CHANGED = "task.status.changed";
    public static final String DELETED = "task.deleted";
    /** 任务字段值变更（含自定义字段） */
    public static final String UPDATED = "task.updated";
    /** 登记/修改工时（预估/实际） */
    public static final String WORKHOUR_LOGGED = "workhour.logged";

    private TaskEvents() {
    }
}
