package com.mido.pm.org.dto;

import java.time.LocalDateTime;

/**
 * 企微配置展示 VO（脱敏）：secret 一律不回明文，仅以 *Secret 布尔位标识「是否已配置」。
 */
public record WecomConfigVO(
        String corpId,
        boolean contactsEnabled,
        boolean contactsSecretSet,
        boolean ssoEnabled,
        String ssoAgentId,
        boolean ssoSecretSet,
        boolean msgEnabled,
        String msgAgentId,
        boolean msgSecretSet,
        LocalDateTime lastSyncAt,
        String lastSyncResult) {
}
