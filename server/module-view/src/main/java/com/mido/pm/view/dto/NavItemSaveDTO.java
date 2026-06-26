package com.mido.pm.view.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 导航编排项（按列表顺序保存为 sort）。parentCode 空=二级，非空=三级。
 * displayName/icon 空则回落 catalog 默认。enabled 默认 true。
 */
public record NavItemSaveDTO(
        @NotBlank(message = "组件编码不能为空") String componentCode,
        String parentCode,
        String displayName,
        String icon,
        Boolean enabled) {
}
