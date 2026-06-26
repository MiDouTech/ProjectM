package com.mido.pm.field.dto;

import java.util.List;

/** 数据源对外视图（含选项集）。 */
public record DataSourceVO(
        Long id,
        String name,
        String groupName,
        String remark,
        String status,
        List<FieldOption> options) {
}
