package com.mido.pm.platform.dto;

/**
 * 平台账号密码强度策略（P0-a）。供各 DTO 的 {@code @Pattern} 复用，避免散落。
 * 规则：8–64 位，且同时包含字母与数字。
 */
public final class PasswordPolicy {

    /** 至少 1 字母 + 1 数字，长度 8–64。 */
    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String MESSAGE = "密码需 8-64 位且同时包含字母和数字";

    private PasswordPolicy() {
    }
}
