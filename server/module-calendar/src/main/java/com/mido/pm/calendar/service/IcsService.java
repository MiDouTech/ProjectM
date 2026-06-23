package com.mido.pm.calendar.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.domain.IcsWriter;
import com.mido.pm.calendar.domain.RecurrenceExpander;
import com.mido.pm.calendar.dto.SubscribeVO;
import com.mido.pm.calendar.entity.PmCalendar;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleException;
import com.mido.pm.calendar.mapper.PmCalendarMapper;
import com.mido.pm.calendar.mapper.PmScheduleExceptionMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日历 ics 订阅服务：为日历惰性生成订阅 token 并产出匿名可拉取的 iCalendar 源。
 * 订阅源按日历所属租户加载日程（匿名无租户上下文，先按 token 全局定位，再切租户），
 * 区间为今日起 {@value #FEED_DAYS} 天（循环已展开）。
 */
@Service
public class IcsService {

    private static final int FEED_DAYS = 90;
    private static final String ICS_PATH = "/api/v1/public/calendars/";

    private final PmCalendarMapper calendarMapper;
    private final PmScheduleMapper scheduleMapper;
    private final PmScheduleExceptionMapper exceptionMapper;

    public IcsService(PmCalendarMapper calendarMapper, PmScheduleMapper scheduleMapper,
                      PmScheduleExceptionMapper exceptionMapper) {
        this.calendarMapper = calendarMapper;
        this.scheduleMapper = scheduleMapper;
        this.exceptionMapper = exceptionMapper;
    }

    /** 为当前用户的日历惰性生成订阅 token，返回订阅地址。 */
    @Transactional(rollbackFor = Exception.class)
    public SubscribeVO ensureSubscribeUrl(Long calendarId) {
        PmCalendar cal = calendarMapper.selectById(calendarId);
        if (cal == null || !UserContext.currentUserId().equals(cal.getOwnerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "日历不存在或无权操作");
        }
        if (cal.getSubscribeToken() == null || cal.getSubscribeToken().isBlank()) {
            cal.setSubscribeToken(IdUtil.fastSimpleUUID());
            calendarMapper.updateById(cal);
        }
        return new SubscribeVO(cal.getSubscribeToken(), ICS_PATH + cal.getSubscribeToken() + "/ics");
    }

    /** 匿名按 token 产出 ics 文本。 */
    public String icsByToken(String token) {
        PmCalendar cal = calendarMapper.selectByTokenGlobal(token);
        if (cal == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订阅链接无效");
        }
        Long prev = TenantContext.get();
        try {
            TenantContext.set(cal.getTenantId());
            LocalDateTime from = LocalDate.now().atStartOfDay();
            LocalDateTime to = from.plusDays(FEED_DAYS);
            List<PmSchedule> schedules = scheduleMapper.selectList(Wrappers.<PmSchedule>lambdaQuery()
                    .eq(PmSchedule::getCalendarId, cal.getId())
                    .eq(PmSchedule::getStatus, "confirmed"));
            Map<Long, List<PmScheduleException>> exBySchedule = exceptionsBySchedule(schedules.stream()
                    .filter(RecurrenceExpander::isRecurring).map(PmSchedule::getId).toList());
            List<IcsWriter.VEvent> events = new ArrayList<>();
            for (PmSchedule s : schedules) {
                if (RecurrenceExpander.isRecurring(s)) {
                    List<PmScheduleException> ex = exBySchedule.getOrDefault(s.getId(), List.of());
                    for (RecurrenceExpander.Occurrence o : RecurrenceExpander.expand(s, ex, from, to)) {
                        events.add(new IcsWriter.VEvent(
                                "sch-" + s.getId() + "-" + o.occurrenceDate() + "@mido",
                                o.title(), o.start(), o.end(), o.location(), isAllDay(s)));
                    }
                } else if (s.getEndTime().isAfter(from) && s.getStartTime().isBefore(to)) {
                    events.add(new IcsWriter.VEvent("sch-" + s.getId() + "@mido",
                            s.getTitle(), s.getStartTime(), s.getEndTime(), s.getLocation(), isAllDay(s)));
                }
            }
            return IcsWriter.write(cal.getName(), events);
        } finally {
            if (prev == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(prev);
            }
        }
    }

    private boolean isAllDay(PmSchedule s) {
        return s.getAllDay() != null && s.getAllDay() == 1;
    }

    /** 一次性按 scheduleId 批量加载例外并分组（避免 N+1）。 */
    private Map<Long, List<PmScheduleException>> exceptionsBySchedule(List<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }
        return exceptionMapper.selectList(Wrappers.<PmScheduleException>lambdaQuery()
                        .in(PmScheduleException::getScheduleId, scheduleIds))
                .stream().collect(Collectors.groupingBy(PmScheduleException::getScheduleId));
    }
}
