package com.mido.pm.approval.dto;

import jakarta.validation.constraints.NotBlank;

/** 创建审批流。definition 为节点 JSON（{"nodes":[...]}）。 */
public record FlowCreateDTO(
        @NotBlank(message = "流程名不能为空") String name,
        String displayName,
        String bizType,
        String mode,
        @NotBlank(message = "流程定义不能为空") String definition) {
}
