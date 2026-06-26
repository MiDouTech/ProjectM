package com.mido.pm.task.dto;

/** 工作项类型对外视图。 */
public record WorkItemTypeVO(
        Long id, String code, String name, String groupName,
        Integer builtin, Integer sort, String status) {
}
