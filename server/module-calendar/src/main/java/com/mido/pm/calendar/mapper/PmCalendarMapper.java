package com.mido.pm.calendar.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.calendar.entity.PmCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** PmCalendar Mapper。 */
@Mapper
public interface PmCalendarMapper extends BaseMapper<PmCalendar> {

    /**
     * 按订阅 token 全局查询（跳过多租户过滤）：ics 匿名订阅无租户上下文，token 全局唯一，
     * 命中后由调用方按 calendar.tenant_id 设置上下文加载日程。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM pm_calendar WHERE subscribe_token = #{token} AND is_deleted = 0 LIMIT 1")
    PmCalendar selectByTokenGlobal(String token);
}
