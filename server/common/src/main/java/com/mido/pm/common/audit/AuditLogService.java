package com.mido.pm.common.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;

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
        AuditLog log = new AuditLog();
        log.setUserId(currentUserId());
        log.setTarget(entityType);
        log.setTargetId(entityId);
        log.setAction(action);
        log.setDetail(writeDetail(detail));
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

    private Long currentUserId() {
        CurrentUser user = UserContext.get();
        return user == null ? null : user.getUserId();
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
        Object detail = null;
        if (log.getDetail() != null) {
            try {
                detail = objectMapper.readValue(log.getDetail(), Object.class);
            } catch (Exception e) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "活动明细解析失败: " + e.getMessage());
            }
        }
        return new ActivityVO(log.getId(), log.getUserId(), log.getAction(), detail, log.getCreateTime());
    }
}
