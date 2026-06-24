package com.mido.pm.project.event;

/**
 * 项目域领域事件名（取自 docs/domain-events.md，集中登记，禁自造）。
 */
public final class ProjectEvents {

    public static final String CREATED = "project.created";
    public static final String STATUS_CHANGED = "project.status.changed";
    public static final String REGISTERED = "project.registered";
    public static final String CLOSED = "project.closed";
    public static final String DELETED = "project.deleted";
    /** 项目字段值变更（含自定义字段） */
    public static final String UPDATED = "project.updated";

    private ProjectEvents() {
    }
}
