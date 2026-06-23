package com.mido.pm.calendar.controller;

import com.mido.pm.calendar.dto.ResourceCreateDTO;
import com.mido.pm.calendar.dto.ResourceVO;
import com.mido.pm.calendar.service.ResourceService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 日历资源：会议室/设备台账（列表 + 新建）。 */
@RestController
@RequestMapping("/api/v1/calendar-resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public R<List<ResourceVO>> list() {
        return R.ok(resourceService.list());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ResourceCreateDTO dto) {
        return R.ok(resourceService.create(dto));
    }
}
