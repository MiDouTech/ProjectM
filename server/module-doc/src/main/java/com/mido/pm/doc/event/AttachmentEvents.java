package com.mido.pm.doc.event;

/** 附件域领域事件名（取自 docs/domain-events.md，集中登记，禁自造）。 */
public final class AttachmentEvents {

    public static final String UPLOADED = "attachment.uploaded";
    public static final String DELETED = "attachment.deleted";

    private AttachmentEvents() {
    }
}
