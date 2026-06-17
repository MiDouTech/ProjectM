package com.mido.pm.approval.domain;

import java.util.List;

/**
 * 审批流定义（approval_flow.definition 的 JSON 结构）。
 */
public record FlowDefinition(List<FlowNode> nodes) {
}
