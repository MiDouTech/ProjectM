package com.mido.pm.calendar.controller;

import com.mido.pm.calendar.dto.RsvpDTO;
import com.mido.pm.calendar.dto.ScheduleCreateDTO;
import com.mido.pm.calendar.dto.ScheduleUpdateDTO;
import com.mido.pm.calendar.dto.ScheduleVO;
import com.mido.pm.calendar.service.ScheduleService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** 日程：CRUD + 按时间段查询（月/周/日视图共用）+ RSVP 反馈。 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 时间段查询：from/to 为 ISO-8601，月/周/日视图按可见区间取数。 */
    @GetMapping
    public R<List<ScheduleVO>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return R.ok(scheduleService.range(from, to));
    }

    @GetMapping("/{id}")
    public R<ScheduleVO> get(@PathVariable Long id) {
        return R.ok(scheduleService.get(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ScheduleCreateDTO dto) {
        return R.ok(scheduleService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ScheduleUpdateDTO dto) {
        scheduleService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return R.ok();
    }

    /** RSVP 反馈：accepted/tentative/declined。 */
    @PostMapping("/{id}/rsvp")
    public R<Void> rsvp(@PathVariable Long id, @Valid @RequestBody RsvpDTO dto) {
        scheduleService.rsvp(id, dto);
        return R.ok();
    }
}
