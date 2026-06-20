package com.mido.pm.platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台超管种子口令回归测试。
 *
 * <p>背景：V28 平台种子曾误用与租户 admin 相同的 BCrypt 哈希（对应明文 admin123），
 * 导致 superadmin 无法以约定口令 superadmin123 登录运营平台。V36 已修正。
 * 本测试锁定「修正后的哈希匹配 superadmin123、旧的错误哈希不匹配」，防止回归。
 */
class PlatformSeedPasswordTest {

    /** V36__fix_platform_admin_password.sql 中写入的 superadmin 正确口令哈希。 */
    private static final String CORRECTED_HASH =
            "$2a$10$yJ7EbOYVM6BXnWNbnZTPSusK25s6dvARL7AK33xgv5oemlZNODk6i";

    /** V28 误用、实际对应明文 admin123 的旧哈希（不应匹配 superadmin123）。 */
    private static final String LEGACY_WRONG_HASH =
            "$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void correctedHashMatchesSuperadminPassword() {
        assertTrue(encoder.matches("superadmin123", CORRECTED_HASH),
                "修正后的哈希应匹配约定口令 superadmin123");
    }

    @Test
    void legacyHashDidNotMatchSuperadminPassword() {
        assertFalse(encoder.matches("superadmin123", LEGACY_WRONG_HASH),
                "旧哈希对应 admin123，不应匹配 superadmin123（即原 bug 根因）");
    }
}
