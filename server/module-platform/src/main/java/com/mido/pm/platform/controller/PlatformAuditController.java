package com.mido.pm.platform.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AuditQueryDTO;
import com.mido.pm.platform.dto.AuditVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 运营审计查询。 */
@RestController
@RequestMapping("/api/v1/platform/audit")
public class PlatformAuditController {

    private final PlatformAuditService auditService;

    public PlatformAuditController(PlatformAuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.AUDIT_QUERY + "')")
    @PostMapping("/query")
    public R<PageResult<AuditVO>> query(@RequestBody AuditQueryDTO query) {
        return R.ok(auditService.query(query));
    }
}
