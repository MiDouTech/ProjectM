package com.mido.pm.project.event;

/**
 * 项目类型域领域事件名（取自 docs/domain-events.md，集中登记，禁自造）。
 */
public final class ProjectTypeEvents {

    public static final String CREATED = "project_type.created";
    public static final String UPDATED = "project_type.updated";
    public static final String DISABLED = "project_type.disabled";

    private ProjectTypeEvents() {
    }
}
