package com.mido.pm.goal.dto;

/** 对齐关系视图。target 名称由前端按 type+id 解析（避免目标域耦合 project/task）。 */
public record AlignmentVO(Long id, Long goalId, String targetType, Long targetId,
                          java.math.BigDecimal weight) {
}
