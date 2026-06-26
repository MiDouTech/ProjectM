package com.mido.pm.common.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 审计日志（活动流）服务。复用平台 sys_audit_log：
 * - {@link #record} 由业务写操作在<strong>同一事务内</strong>调用，记录可读变更（谁/何时/改了什么/X→Y）；
 * - {@link #query} 供 GET /{entity}/{id}/activities 分页倒序读取。
 *
 * <p>detail 的读写均走共享 ObjectMapper（含全局 Long→String），保证明细里的 ID 与对外接口一致，
 * 避免雪花 ID 在 detail 中以数字下发被前端丢精度。
 */
@Service
public class AuditLogService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一条活动。须在调用方事务内执行，与业务变更同生共死。
     *
     * @param entityType 实体类型（AuditActions.TARGET_*）
     * @param entityId   实体主键
     * @param action     动作码（AuditActions.*）
     * @param detail     变更明细对象（自动序列化为 JSON），无明细传 null
     */
    public void record(String entityType, Long entityId, String action, Object detail) {
        record(null, entityType, entityId, action, detail);
    }

    /**
     * 记录一条活动（带功能模块维度，供管理后台「操作日志」分组过滤）。须在调用方事务内执行。
     * 操作人取自当前安全上下文，IP / User-Agent 自动从当前请求 best-effort 捕获。
     *
     * @param module     功能模块（AuditActions.MODULE_*），活动流场景可传 null
     * @param entityType 实体类型（AuditActions.TARGET_*）
     * @param entityId   实体主键
     * @param action     动作码（AuditActions.*）
     * @param detail     变更明细对象（自动序列化为 JSON），无明细传 null
     */
    public void record(String module, String entityType, Long entityId, String action, Object detail) {
        AuditLog log = new AuditLog();
        log.setUserId(currentUserId());
        log.setModule(module);
        log.setTarget(entityType);
        log.setTargetId(entityId);
        log.setAction(action);
        log.setDetail(writeDetail(detail));
        log.setIp(clientIp());
        log.setUserAgent(userAgent());
        auditLogMapper.insert(log);
    }

    /** 分页倒序查询某实体的活动流。 */
    public PageResult<ActivityVO> query(String entityType, Long entityId, Long page, Long size) {
        long pageNo = page == null || page < 1 ? 1 : page;
        long sz = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Page<AuditLog> p = new Page<>(pageNo, sz);
        Page<AuditLog> result = auditLogMapper.selectPage(p, Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getTarget, entityType)
                .eq(AuditLog::getTargetId, entityId)
                .orderByDesc(AuditLog::getId));
        List<ActivityVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, sz);
    }

    /**
     * 管理后台操作日志分页查询（倒序）。租户隔离由拦截器注入，按条件过滤。
     * 返回的 VO 不含 userName（common 不依赖 org），由调用方按 userId 反查回填。
     */
    public PageResult<AuditLogVO> queryLogs(AuditLogQueryDTO q) {
        long pageNo = q.page() == null || q.page() < 1 ? 1 : q.page();
        long sz = q.size() == null || q.size() < 1 ? 20 : Math.min(q.size(), MAX_PAGE_SIZE);
        Page<AuditLog> p = new Page<>(pageNo, sz);
        Page<AuditLog> result = auditLogMapper.selectPage(p, Wrappers.<AuditLog>lambdaQuery()
                .eq(q.userId() != null, AuditLog::getUserId, q.userId())
                .eq(StringUtils.hasText(q.module()), AuditLog::getModule, q.module())
                .eq(StringUtils.hasText(q.action()), AuditLog::getAction, q.action())
                .eq(StringUtils.hasText(q.target()), AuditLog::getTarget, q.target())
                .eq(q.targetId() != null, AuditLog::getTargetId, q.targetId())
                .ge(q.startTime() != null, AuditLog::getCreateTime, q.startTime())
                .le(q.endTime() != null, AuditLog::getCreateTime, q.endTime())
                .orderByDesc(AuditLog::getId));
        List<AuditLogVO> list = result.getRecords().stream().map(this::toLogVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, sz);
    }

    private AuditLogVO toLogVO(AuditLog log) {
        return new AuditLogVO(log.getId(), log.getUserId(), null, log.getModule(), log.getAction(),
                log.getTarget(), log.getTargetId(), readDetail(log.getDetail()), log.getIp(), log.getCreateTime());
    }

    private Long currentUserId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.getUserId();
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 256) {
            return ua.substring(0, 256);
        }
        return ua;
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String writeDetail(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "活动明细序列化失败: " + e.getMessage());
        }
    }

    private ActivityVO toVO(AuditLog log) {
        return new ActivityVO(log.getId(), log.getUserId(), log.getAction(),
                readDetail(log.getDetail()), log.getCreateTime());
    }

    private Object readDetail(String detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.readValue(detail, Object.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "活动明细解析失败: " + e.getMessage());
        }
    }
}
