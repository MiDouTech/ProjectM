package com.mido.pm.calendar.controller;

import com.mido.pm.calendar.dto.CalendarVO;
import com.mido.pm.calendar.service.CalendarService;
import com.mido.pm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 日历容器：当前用户的日历列表（含默认「我的日程」）。 */
@RestController
@RequestMapping("/api/v1/calendars")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public R<List<CalendarVO>> listMine() {
        return R.ok(calendarService.listMine());
    }
}
