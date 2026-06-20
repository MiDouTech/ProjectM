package com.mido.pm.config;

import com.mido.pm.security.ImpersonationReadOnlyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册模拟登录只读拦截器，作用于全部 /api/v1/** 接口。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ImpersonationReadOnlyInterceptor impersonationReadOnlyInterceptor;

    public WebMvcConfig(ImpersonationReadOnlyInterceptor impersonationReadOnlyInterceptor) {
        this.impersonationReadOnlyInterceptor = impersonationReadOnlyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(impersonationReadOnlyInterceptor).addPathPatterns("/api/v1/**");
    }
}
