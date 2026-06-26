package com.mido.pm.org.service;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.audit.AuditLogQueryDTO;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.audit.AuditLogVO;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 管理后台操作日志查询：在 common 的 {@link AuditLogService#queryLogs} 之上回填操作人姓名。
 * common 不依赖 org，故名称反查放在 org 层（与平台审计回填管理员名同思路）。
 */
@Service
public class AuditQueryService {

    private final AuditLogService auditLogService;
    private final SysUserMapper userMapper;

    public AuditQueryService(AuditLogService auditLogService, SysUserMapper userMapper) {
        this.auditLogService = auditLogService;
        this.userMapper = userMapper;
    }

    public PageResult<AuditLogVO> query(AuditLogQueryDTO q) {
        PageResult<AuditLogVO> page = auditLogService.queryLogs(q);
        Map<Long, String> names = loadUserNames(page.getList());
        List<AuditLogVO> enriched = page.getList().stream()
                .map(v -> v.withUserName(names.get(v.userId())))
                .toList();
        return PageResult.of(enriched, page.getTotal(), page.getPage(), page.getSize());
    }

    private Map<Long, String> loadUserNames(List<AuditLogVO> logs) {
        List<Long> ids = logs.stream().map(AuditLogVO::userId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(ids)) {
                names.put(u.getId(), u.getName());
            }
        }
        return names;
    }
}
