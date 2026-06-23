package com.mido.pm.org.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.org.entity.SysIdentityMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** SysIdentityMap Mapper。 */
@Mapper
public interface SysIdentityMapMapper extends BaseMapper<SysIdentityMap> {

    /**
     * 按 provider + external_id 全局查询（跳过多租户过滤）：SSO 匿名登录无租户上下文，
     * external_id 全局唯一，命中后据 row.tenant_id 签发令牌。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_identity_map WHERE provider = #{provider} AND external_id = #{externalId} "
            + "AND is_deleted = 0 LIMIT 1")
    SysIdentityMap selectByExternalGlobal(@Param("provider") String provider, @Param("externalId") String externalId);
}
