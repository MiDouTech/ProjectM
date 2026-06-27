package com.mido.pm.platform.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AuditQueryDTO;
import com.mido.pm.platform.dto.AuditVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 运营审计查询。 */
@Tag(name = "平台-运营审计", description = "敏感运营动作留痕查询")
@RestController
@RequestMapping("/api/v1/platform/audit")
public class PlatformAuditController {

    private final PlatformAuditService auditService;

    public PlatformAuditController(PlatformAuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.AUDIT_QUERY + "')")
    @Operation(summary = "审计分页查询", description = "按对象类型/动作过滤，倒序")
    @PostMapping("/query")
    public R<PageResult<AuditVO>> query(@RequestBody AuditQueryDTO query) {
        return R.ok(auditService.query(query));
    }
}
