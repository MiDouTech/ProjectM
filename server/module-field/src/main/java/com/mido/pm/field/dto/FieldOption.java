package com.mido.pm.field.dto;

import jakarta.validation.constraints.NotBlank;

/** 选项项（select/multi_select 用）。value 入库存储值，label 显示文案。 */
public record FieldOption(
        @NotBlank(message = "选项值不能为空") String value,
        @NotBlank(message = "选项文案不能为空") String label) {
}
