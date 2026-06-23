package com.mido.pm.calendar.controller;

import com.mido.pm.calendar.service.IcsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日历 ics 匿名订阅（/api/v1/public/** 免鉴权）。返回原始 text/calendar，不走统一响应包装，
 * 供 Google/Outlook/Apple 等外部客户端按 URL 订阅。
 */
@RestController
@RequestMapping("/api/v1/public/calendars")
public class PublicCalendarController {

    private final IcsService icsService;

    public PublicCalendarController(IcsService icsService) {
        this.icsService = icsService;
    }

    @GetMapping(value = "/{token}/ics", produces = "text/calendar;charset=UTF-8")
    public String ics(@PathVariable String token) {
        return icsService.icsByToken(token);
    }
}
