package com.mido.pm.org.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.common.audit.AuditLogQueryDTO;
import com.mido.pm.common.audit.AuditLogVO;
import com.mido.pm.org.service.AuditQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理后台「操作日志」查询。仅管理员可见（敏感合规数据）：
 * 接受专用权限 org:audit:query，或回退到既有管理员权限（建/管角色或用户者）。
 * 复杂过滤走 POST /query（与 /users/query 一致，见 api-conventions）。
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    public AuditLogController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @PreAuthorize("hasAnyAuthority('org:audit:query','org:role:create','org:user:create')")
    @PostMapping("/query")
    public R<PageResult<AuditLogVO>> query(@RequestBody AuditLogQueryDTO query) {
        return R.ok(auditQueryService.query(query));
    }
}
