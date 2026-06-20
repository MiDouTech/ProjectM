package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** 编辑租户基础信息（编码与状态不在此改：编码不可变，状态走专用接口）。 */
public record TenantUpdateDTO(
        @NotBlank(message = "租户名称不能为空") String name,
        String industry,
        String contactName,
        String contactPhone,
        String contactEmail,
        String remark) {
}
