package com.mido.pm.change.dto;

/**
 * 变更策略视图：某变更类型是否必审、绑定的审批流、是否启用。
 * changeType 文案与 flow 名称由前端字典/审批流列表解析。
 */
public record ChangePolicyVO(
        Long id,
        String changeType,
        Integer requireApproval,
        Long flowId,
        Integer enabled) {
}
