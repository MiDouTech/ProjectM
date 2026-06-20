package com.mido.pm.org.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 组织域物理清除（注销合规）：按 tenant_id 物理删除组织/权限相关表。
 * 忽略多租户拦截器、显式 tenant 过滤；物理删除不走逻辑删除。
 */
@Mapper
public interface OrgPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_user WHERE tenant_id = #{t}")
    int purgeUsers(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_dept WHERE tenant_id = #{t}")
    int purgeDepts(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_role WHERE tenant_id = #{t}")
    int purgeRoles(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_user_role WHERE tenant_id = #{t}")
    int purgeUserRoles(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_role_perm WHERE tenant_id = #{t}")
    int purgeRolePerms(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_role_data_scope WHERE tenant_id = #{t}")
    int purgeRoleDataScopes(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_identity_map WHERE tenant_id = #{t}")
    int purgeIdentityMaps(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM sys_api_key WHERE tenant_id = #{t}")
    int purgeApiKeys(@Param("t") Long tenantId);
}
