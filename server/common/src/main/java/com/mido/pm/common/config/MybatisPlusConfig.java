package com.mido.pm.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mido.pm.common.datascope.DataScopeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 核心配置：多租户 + 数据范围 + 分页拦截器。
 * 顺序约定：多租户 → 数据范围 → 分页（条件类拦截器排在分页之前）。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 多租户（自动注入 tenant_id）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new MidoTenantLineHandler()));
        // 2. 数据范围（按当前用户角色 sys_role_data_scope 注入查询条件）
        interceptor.addInnerInterceptor(new DataScopeInterceptor());
        // 3. 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
