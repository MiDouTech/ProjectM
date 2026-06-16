package com.mido.pm.approval.dto;

public record InstanceVO(
        Long id,
        Long flowId,
        String bizType,
        Long bizId,
        String status,
        String currentNode,
        Long applicantId) {
}
