package com.mido.pm.common.audit;

import java.time.LocalDateTime;

/**
 * 管理后台操作日志查询条件。所有过滤项可空（空则不约束）。
 * 租户隔离由多租户拦截器统一注入，无需手写 tenant 条件。
 *
 * @param userId    操作人用户 ID
 * @param module    功能模块（AuditActions.MODULE_*）
 * @param action    动作码（AuditActions.*）
 * @param target    实体类型（AuditActions.TARGET_*）
 * @param targetId  实体主键
 * @param startTime 起始时间（含）
 * @param endTime   结束时间（含）
 * @param page      页码（从 1 起）
 * @param size      每页条数（上限 100）
 */
public record AuditLogQueryDTO(
        Long userId,
        String module,
        String action,
        String target,
        Long targetId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long page,
        Long size) {
}
