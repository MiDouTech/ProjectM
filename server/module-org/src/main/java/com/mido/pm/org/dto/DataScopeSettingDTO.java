package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

/** 角色数据范围设置项。scope 取 self/dept/dept_and_sub/all/custom。 */
public record DataScopeSettingDTO(
        @NotBlank(message = "资源标识不能为空") String resource,
        @NotBlank(message = "数据范围不能为空") String scope) {
}
