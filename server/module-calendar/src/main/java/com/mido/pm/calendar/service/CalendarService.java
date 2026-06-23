package com.mido.pm.calendar.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.dto.CalendarVO;
import com.mido.pm.calendar.entity.PmCalendar;
import com.mido.pm.calendar.mapper.PmCalendarMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 日历容器服务：维护用户日历列表，惰性保证默认「我的日程」日历存在。
 * 日程归属解析（calendarId 为空→默认日历）也在此提供。
 */
@Service
public class CalendarService {

    static final String DEFAULT_CALENDAR_NAME = "我的日程";

    private final PmCalendarMapper calendarMapper;

    public CalendarService(PmCalendarMapper calendarMapper) {
        this.calendarMapper = calendarMapper;
    }

    /** 当前用户的日历列表（默认「我的日程」置顶），惰性创建默认日历。 */
    public List<CalendarVO> listMine() {
        ensureDefaultCalendarId();
        return calendarMapper.selectList(Wrappers.<PmCalendar>lambdaQuery()
                        .eq(PmCalendar::getOwnerId, currentUserId())
                        .eq(PmCalendar::getStatus, "active")
                        .orderByDesc(PmCalendar::getIsDefault)
                        .orderByAsc(PmCalendar::getId))
                .stream().map(this::toVO).toList();
    }

    /**
     * 解析日程归属日历：传入为空时落到当前用户默认日历；传入则校验归属当前用户。
     *
     * @return 有效 calendarId
     */
    @Transactional(rollbackFor = Exception.class)
    public Long resolveCalendarId(Long calendarId) {
        if (calendarId == null) {
            return ensureDefaultCalendarId();
        }
        PmCalendar cal = calendarMapper.selectById(calendarId);
        if (cal == null || !currentUserId().equals(cal.getOwnerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "日历不存在或无权使用");
        }
        return calendarId;
    }

    /** 返回当前用户默认「我的日程」日历 id，不存在则创建。 */
    @Transactional(rollbackFor = Exception.class)
    public Long ensureDefaultCalendarId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        PmCalendar existing = calendarMapper.selectOne(Wrappers.<PmCalendar>lambdaQuery()
                .eq(PmCalendar::getOwnerId, userId)
                .eq(PmCalendar::getIsDefault, 1)
                .last("limit 1"));
        if (existing != null) {
            return existing.getId();
        }
        PmCalendar cal = new PmCalendar();
        cal.setName(DEFAULT_CALENDAR_NAME);
        cal.setType("personal");
        cal.setOwnerId(userId);
        cal.setVisibility("private");
        cal.setIsDefault(1);
        cal.setStatus("active");
        calendarMapper.insert(cal);
        return cal.getId();
    }

    /** 当前用户拥有的日历 id 列表（用于日程范围查询）。 */
    public List<Long> myCalendarIds() {
        return calendarMapper.selectList(Wrappers.<PmCalendar>lambdaQuery()
                        .select(PmCalendar::getId)
                        .eq(PmCalendar::getOwnerId, currentUserId()))
                .stream().map(PmCalendar::getId).toList();
    }

    private CalendarVO toVO(PmCalendar c) {
        return new CalendarVO(c.getId(), c.getName(), c.getType(), c.getOwnerId(),
                c.getColor(), c.getVisibility(), c.getIsDefault(), c.getStatus());
    }

    private Long currentUserId() {
        return UserContext.currentUserId();
    }
}
