package com.mido.pm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全基座（不依赖 provider/业务模块，避免分层成环）：
 * 提供 {@link PasswordEncoder}，并开启方法级 {@code @PreAuthorize} 鉴权。
 * 具体的 JWT 过滤链在 bootstrap（组合根）定义，因其需同时使用 SsoProvider 与 IdentityProvider。
 */
@Configuration
@EnableMethodSecurity
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
