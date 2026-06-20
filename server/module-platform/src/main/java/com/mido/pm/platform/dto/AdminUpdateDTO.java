package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 编辑平台运营账号（姓名/状态/角色）。 */
public record AdminUpdateDTO(
        @NotBlank(message = "姓名不能为空") String name,
        @NotBlank(message = "状态不能为空") String status,
        @NotEmpty(message = "至少分配一个角色") List<Long> roleIds) {
}
