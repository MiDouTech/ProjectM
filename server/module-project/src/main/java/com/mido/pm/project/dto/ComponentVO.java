package com.mido.pm.project.dto;

/** 组件库条目（catalog）。 */
public record ComponentVO(
        Long id, String code, String name, String icon,
        Integer multiInstance, Integer builtin, Integer sort) {
}
