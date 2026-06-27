package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.AuditQueryDTO;
import com.mido.pm.platform.dto.AuditVO;
import com.mido.pm.platform.entity.SysPlatformAdmin;
import com.mido.pm.platform.entity.SysPlatformAuditLog;
import com.mido.pm.platform.mapper.SysPlatformAdminMapper;
import com.mido.pm.platform.mapper.SysPlatformAuditLogMapper;
import com.mido.pm.platform.security.PlatformContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台运营审计服务：记录敏感运营动作 + 分页查询。
 * 操作人取自 {@link PlatformContext}，IP 取自当前请求（best-effort）。
 */
@Service
public class PlatformAuditService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SysPlatformAuditLogMapper auditMapper;
    private final SysPlatformAdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    public PlatformAuditService(SysPlatformAuditLogMapper auditMapper,
                                SysPlatformAdminMapper adminMapper,
                                ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.adminMapper = adminMapper;
        this.objectMapper = objectMapper;
    }

    /** 记录一条运营审计（操作人取自 PlatformContext）。须在调用方事务内执行（与业务变更同生共死）。 */
    public void record(String action, String target, Long targetId, Object detail) {
        record(PlatformContext.currentAdminId(), action, target, targetId, detail);
    }

    /** 记录一条运营审计，显式指定操作人（用于登录等无 PlatformContext 的场景）。 */
    public void record(Long actorAdminId, String action, String target, Long targetId, Object detail) {
        SysPlatformAuditLog log = new SysPlatformAuditLog();
        log.setAdminId(actorAdminId);
        log.setAction(action);
        log.setTarget(target);
        log.setTargetId(targetId);
        log.setDetail(writeDetail(detail));
        log.setIp(clientIp());
        auditMapper.insert(log);
    }

    /** 分页倒序查询运营审计。 */
    public PageResult<AuditVO> query(AuditQueryDTO q) {
        long pageNo = q.page() == null || q.page() < 1 ? 1 : q.page();
        long size = q.size() == null || q.size() < 1 ? 20 : Math.min(q.size(), MAX_PAGE_SIZE);
        Page<SysPlatformAuditLog> page = new Page<>(pageNo, size);
        Page<SysPlatformAuditLog> result = auditMapper.selectPage(page, Wrappers.<SysPlatformAuditLog>lambdaQuery()
                .eq(StringUtils.hasText(q.target()), SysPlatformAuditLog::getTarget, q.target())
                .eq(StringUtils.hasText(q.action()), SysPlatformAuditLog::getAction, q.action())
                .orderByDesc(SysPlatformAuditLog::getId));
        Map<Long, String> adminNames = loadAdminNames(result.getRecords());
        List<AuditVO> list = result.getRecords().stream()
                .map(l -> toVO(l, adminNames)).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    private Map<Long, String> loadAdminNames(List<SysPlatformAuditLog> logs) {
        List<Long> ids = logs.stream().map(SysPlatformAuditLog::getAdminId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            adminMapper.selectBatchIds(ids).forEach(a -> names.put(a.getId(), a.getName()));
        }
        return names;
    }

    private AuditVO toVO(SysPlatformAuditLog log, Map<Long, String> adminNames) {
        Object detail = null;
        if (log.getDetail() != null) {
            try {
                detail = objectMapper.readValue(log.getDetail(), Object.class);
            } catch (Exception e) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "审计明细解析失败: " + e.getMessage());
            }
        }
        return new AuditVO(log.getId(), log.getAdminId(), adminNames.get(log.getAdminId()),
                log.getAction(), log.getTarget(), log.getTargetId(), detail, log.getIp(), log.getCreateTime());
    }

    private String writeDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "审计明细序列化失败: " + e.getMessage());
        }
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
