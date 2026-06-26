package com.mido.pm.task.dto;

/** 关联定义对外视图（含类型名）。 */
public record RelationDefVO(
        Long id,
        Long sourceTypeId, String sourceTypeName,
        Long targetTypeId, String targetTypeName,
        String relationKind, String name) {
}
