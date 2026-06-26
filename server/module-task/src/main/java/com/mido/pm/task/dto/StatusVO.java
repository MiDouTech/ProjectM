package com.mido.pm.task.dto;

/** 状态库对外视图。 */
public record StatusVO(
        Long id, String name, String color, String metaCategory,
        String groupName, Integer sort, Integer builtin, String status) {
}
