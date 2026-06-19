package com.mido.pm.doc.event;

/** 文档域领域事件名（取自 docs/domain-events.md §5.1，集中登记，禁自造）。 */
public final class DocEvents {

    public static final String CREATED = "doc.created";
    public static final String UPDATED = "doc.updated";
    public static final String VERSION_CREATED = "doc.version.created";
    public static final String MOVED = "doc.moved";
    public static final String DELETED = "doc.deleted";

    private DocEvents() {
    }
}
