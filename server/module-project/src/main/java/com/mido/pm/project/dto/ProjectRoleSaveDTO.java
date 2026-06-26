package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 项目角色创建/更新入参。code 创建后不可改（以路径 id 为准）。
 */
public record ProjectRoleSaveDTO(
        @NotBlank(message = "角色编码不能为空") String code,
        @NotBlank(message = "角色名不能为空") String name,
        Integer sort,
        String status) {
}
