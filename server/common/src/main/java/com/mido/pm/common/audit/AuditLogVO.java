package com.mido.pm.common.audit;

import java.time.LocalDateTime;

/**
 * 管理后台操作日志条目（对外）：谁(userId/userName) 在何时(createTime) 从何处(ip)
 * 对哪个模块(module) 的哪个实体(target/targetId) 做了什么(action)，变更明细(detail)。
 * userName 由 org 层按 userId 反查填充（common 不依赖 org，故此处可为空）。
 */
public record AuditLogVO(
        Long id,
        Long userId,
        String userName,
        String module,
        String action,
        String target,
        Long targetId,
        Object detail,
        String ip,
        LocalDateTime createTime) {

    /** 以已解析的 userName 复制一份（用于 org 层名称回填）。 */
    public AuditLogVO withUserName(String name) {
        return new AuditLogVO(id, userId, name, module, action, target, targetId, detail, ip, createTime);
    }
}
