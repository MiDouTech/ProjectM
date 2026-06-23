package com.mido.pm.briefing.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 自定义模板保存。type：daily/weekly/monthly；fields 为字段定义。 */
public record TemplateSaveDTO(
        @NotBlank(message = "模板名称不能为空") String name,
        @NotBlank(message = "类型不能为空") String type,
        List<FieldDefVO> fields) {
}
