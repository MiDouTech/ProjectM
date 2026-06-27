package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AnnouncementSaveDTO;
import com.mido.pm.platform.dto.AnnouncementVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "平台-公告管理", description = "运营公告建/发布/下线")
@RestController
@RequestMapping("/api/v1/platform/announcements")
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService announcementService;

    public PlatformAnnouncementController(PlatformAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @Operation(summary = "公告列表")
    @GetMapping
    public R<List<AnnouncementVO>> list() {
        return R.ok(announcementService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @Operation(summary = "新建公告")
    @PostMapping
    public R<Long> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        return R.ok(announcementService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementSaveDTO dto) {
        announcementService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ANNOUNCEMENT_MANAGE + "')")
    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}
