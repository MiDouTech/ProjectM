package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

public record DeptCreateDTO(
        @NotBlank(message = "部门名称不能为空") String name,
        Long parentId) {
}
