package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** 创建 API Key 入参。绑定到当前登录用户；expireAt 可空表示长期有效。 */
public record ApiKeyCreateDTO(
        @NotBlank(message = "名称不能为空") String name,
        LocalDateTime expireAt) {
}
