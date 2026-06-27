package com.mido.pm.platform.security;

import java.util.List;

/**
 * 当前登录的平台运营人员主体。由平台认证过滤器装配并写入 {@link PlatformContext}，
 * 权限码同时作为 Spring Security 权限供 {@code @PreAuthorize} 鉴权。
 *
 * @param adminId            平台账号 ID
 * @param username           登录名
 * @param name               显示名
 * @param permCodes          已合并的权限码集合
 * @param mustChangePassword 是否需强制改密（首登/被重置后）
 */
public record PlatformPrincipal(Long adminId, String username, String name, List<String> permCodes,
                               boolean mustChangePassword) {
}
