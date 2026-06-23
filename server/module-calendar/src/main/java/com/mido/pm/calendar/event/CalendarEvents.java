package com.mido.pm.calendar.event;

/** 日历/日程域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class CalendarEvents {

    public static final String SCHEDULE_CREATED = "calendar.schedule.created";
    public static final String SCHEDULE_UPDATED = "calendar.schedule.updated";
    public static final String SCHEDULE_DELETED = "calendar.schedule.deleted";
    public static final String RSVP_RESPONDED = "calendar.rsvp.responded";

    private CalendarEvents() {
    }
}
