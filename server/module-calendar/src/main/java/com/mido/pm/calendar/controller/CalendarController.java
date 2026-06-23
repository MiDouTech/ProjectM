package com.mido.pm.calendar.controller;

import com.mido.pm.calendar.dto.CalendarVO;
import com.mido.pm.calendar.dto.SubscribeVO;
import com.mido.pm.calendar.service.CalendarService;
import com.mido.pm.calendar.service.IcsService;
import com.mido.pm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 日历容器：当前用户的日历列表（含默认「我的日程」）+ ics 订阅地址。 */
@RestController
@RequestMapping("/api/v1/calendars")
public class CalendarController {

    private final CalendarService calendarService;
    private final IcsService icsService;

    public CalendarController(CalendarService calendarService, IcsService icsService) {
        this.calendarService = calendarService;
        this.icsService = icsService;
    }

    @GetMapping
    public R<List<CalendarVO>> listMine() {
        return R.ok(calendarService.listMine());
    }

    /** 获取/生成日历 ics 订阅地址（仅日历归属者）。 */
    @PostMapping("/{id}/subscribe")
    public R<SubscribeVO> subscribe(@PathVariable Long id) {
        return R.ok(icsService.ensureSubscribeUrl(id));
    }
}
