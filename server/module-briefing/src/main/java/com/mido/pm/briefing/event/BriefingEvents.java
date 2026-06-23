package com.mido.pm.briefing.event;

/** 简报域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class BriefingEvents {

    public static final String SUBMITTED = "briefing.submitted";
    public static final String REVIEWED = "briefing.reviewed";
    public static final String REMINDER_DUE = "briefing.reminder.due";
    public static final String ISSUE_RAISED = "briefing.issue.raised";
    public static final String ISSUE_CLOSED = "briefing.issue.closed";

    private BriefingEvents() {
    }
}
