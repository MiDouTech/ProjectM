package com.mido.pm.project.dto;

/** 项目类型视图。 */
public record ProjectTypeVO(
        Long id,
        String code,
        String name,
        String parentCode,
        String color,
        String icon,
        Integer sort,
        String minJobLevel,
        Integer requiresNpss,
        Long defaultFlowId,
        Integer requireGoalAlignment,
        String stakeholderTpl,
        String status,
        String description) {
}
