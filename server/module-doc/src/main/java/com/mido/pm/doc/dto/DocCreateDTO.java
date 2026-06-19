package com.mido.pm.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 新建文档/目录节点。type=folder/doc；parentId 缺省为 0（根）。 */
public record DocCreateDTO(
        @NotNull(message = "项目不能为空") Long projectId,
        Long parentId,
        @NotBlank(message = "类型不能为空") String type,
        @NotBlank(message = "标题不能为空") String title,
        String icon) {
}
