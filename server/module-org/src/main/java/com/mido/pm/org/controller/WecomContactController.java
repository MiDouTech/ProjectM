package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.WecomSyncResultVO;
import com.mido.pm.org.service.WecomContactSyncService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 企微通讯录：手动触发全量同步（部门/成员 → sys_dept/sys_user + sys_identity_map）。 */
@RestController
@RequestMapping("/api/v1/wecom/contacts")
public class WecomContactController {

    private final WecomContactSyncService syncService;

    public WecomContactController(WecomContactSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('org:user:create')")
    public R<WecomSyncResultVO> sync() {
        return R.ok(syncService.sync());
    }
}
