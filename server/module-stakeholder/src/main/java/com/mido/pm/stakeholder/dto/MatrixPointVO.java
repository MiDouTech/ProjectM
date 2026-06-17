package com.mido.pm.stakeholder.dto;

/** 权力利益矩阵点：四象限渲染数据。quadrant 为象限名。 */
public record MatrixPointVO(
        Long stakeholderId,
        String name,
        String role,
        Integer powerLevel,
        Integer interestLevel,
        String quadrant) {
}
