package com.mido.pm.stakeholder.dto;

import java.math.BigDecimal;

public record StakeholderVO(
        Long id,
        Long projectId,
        Long userId,
        String externalName,
        String role,
        String category,
        Integer powerLevel,
        Integer interestLevel,
        BigDecimal npssWeight) {
}
