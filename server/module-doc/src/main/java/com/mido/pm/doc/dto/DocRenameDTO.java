package com.mido.pm.doc.dto;

import jakarta.validation.constraints.NotBlank;

/** 重命名/改图标。 */
public record DocRenameDTO(
        @NotBlank(message = "标题不能为空") String title,
        String icon) {
}
