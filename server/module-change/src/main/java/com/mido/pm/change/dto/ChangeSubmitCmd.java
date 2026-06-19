package com.mido.pm.change.dto;

import java.util.Map;

/**
 * 提交变更单命令（被改业务域→变更域的内部入参）。before/after 为已序列化的 JSON 文本，
 * 由被改域负责快照（变更域不耦合各域字段）；formData 供审批条件路由/guard 上下文。
 */
public record ChangeSubmitCmd(
        String bizType,
        Long bizId,
        String changeType,
        String title,
        String reason,
        String impact,
        String beforeSnapshot,
        String afterPayload,
        Map<String, Object> formData) {
}
