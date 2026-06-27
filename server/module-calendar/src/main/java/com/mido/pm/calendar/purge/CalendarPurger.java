package com.mido.pm.calendar.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 日历/日程域数据清除。 */
@Component
public class CalendarPurger implements TenantDataPurger {

    private final CalendarPurgeMapper mapper;

    public CalendarPurger(CalendarPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "calendar";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeReminderLogs(tenantId)
                + mapper.purgeParticipants(tenantId)
                + mapper.purgeExceptions(tenantId)
                + mapper.purgeScheduleResources(tenantId)
                + mapper.purgeSchedules(tenantId)
                + mapper.purgeCalendarResources(tenantId)
                + mapper.purgeCalendars(tenantId);
    }
}
