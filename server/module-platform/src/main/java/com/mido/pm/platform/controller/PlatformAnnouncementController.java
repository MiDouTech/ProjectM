package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AnnouncementSaveDTO;
import com.mido.pm.platform.dto.AnnouncementVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公告管理（运营侧）。 */
@RestController
@RequestMapping("/api/v1/platform/announcements")
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService announcementService;

    public PlatformAnnouncementController(PlatformAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @GetMapping
    public R<List<AnnouncementVO>> list() {
        return R.ok(announcementService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        return R.ok(announcementService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementSaveDTO dto) {
        announcementService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}
