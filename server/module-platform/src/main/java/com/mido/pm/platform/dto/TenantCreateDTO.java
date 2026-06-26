package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 开通租户入参。code 为租户编码（程序引用/子域名），仅允许小写字母数字与连字符。
 * planId/expireAt 可空：留空表示先开通不绑定套餐，后续在订阅中绑定。
 * adminUsername/adminPassword 为租户初始管理员凭据（开通时播种，使租户开通即可登录管理）：
 * 留空则用默认 admin / {@code Mido@2024}，运营须提示客户首次登录后修改。
 */
public record TenantCreateDTO(
        @NotBlank(message = "租户名称不能为空") String name,
        @NotBlank(message = "租户编码不能为空")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,31}$", message = "编码为2-32位小写字母/数字/连字符且不以连字符开头") String code,
        String industry,
        String contactName,
        String contactPhone,
        String contactEmail,
        String remark,
        String adminUsername,
        String adminPassword) {
}
