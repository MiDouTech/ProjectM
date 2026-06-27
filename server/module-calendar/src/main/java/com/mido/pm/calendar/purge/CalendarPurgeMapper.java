package com.mido.pm.calendar.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 日历/日程域物理清除（注销合规）。子表先于主表。 */
@Mapper
public interface CalendarPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_schedule_reminder_log WHERE tenant_id = #{t}")
    int purgeReminderLogs(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_schedule_participant WHERE tenant_id = #{t}")
    int purgeParticipants(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_schedule_exception WHERE tenant_id = #{t}")
    int purgeExceptions(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_schedule_resource WHERE tenant_id = #{t}")
    int purgeScheduleResources(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_schedule WHERE tenant_id = #{t}")
    int purgeSchedules(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_calendar_resource WHERE tenant_id = #{t}")
    int purgeCalendarResources(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_calendar WHERE tenant_id = #{t}")
    int purgeCalendars(@Param("t") Long tenantId);
}
