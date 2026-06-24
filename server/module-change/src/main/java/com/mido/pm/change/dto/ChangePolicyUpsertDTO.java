package com.mido.pm.change.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 变更策略保存（按 changeType 幂等 upsert）。
 * 必审（requireApproval=1）时 flowId 必填——否则提交变更会因无审批流被拒（杜绝 goal_target 那类隐患）。
 */
public record ChangePolicyUpsertDTO(
        @NotBlank(message = "变更类型必填") String changeType,
        Integer requireApproval,
        Long flowId,
        Integer enabled) {
}
