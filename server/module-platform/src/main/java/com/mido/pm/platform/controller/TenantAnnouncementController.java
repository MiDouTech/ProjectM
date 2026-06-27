package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AnnouncementVO;
import com.mido.pm.platform.service.PlatformAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户侧公告读取（走租户安全链，需租户登录态）。返回当前生效公告。
 * 路径不在 /platform/** 下，故由租户应用链鉴权。
 */
@Tag(name = "租户-公告", description = "租户侧读取当前生效公告")
@RestController
@RequestMapping("/api/v1/announcements")
public class TenantAnnouncementController {

    private final PlatformAnnouncementService announcementService;

    public TenantAnnouncementController(PlatformAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "当前生效公告", description = "供租户顶栏公告入口展示")
    @GetMapping
    public R<List<AnnouncementVO>> active() {
        return R.ok(announcementService.activeAnnouncements());
    }
}
