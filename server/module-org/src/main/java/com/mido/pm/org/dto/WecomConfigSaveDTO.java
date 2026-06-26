package com.mido.pm.org.dto;

/**
 * 企微配置保存 DTO。secret 字段语义：传非空=更新为新值（加密入库）；传空=保持原值不变（避免脱敏回显覆盖）。
 */
public record WecomConfigSaveDTO(
        String corpId,
        Boolean contactsEnabled,
        String contactsSecret,
        Boolean ssoEnabled,
        String ssoAgentId,
        String ssoSecret,
        Boolean msgEnabled,
        String msgAgentId,
        String msgSecret) {
}
