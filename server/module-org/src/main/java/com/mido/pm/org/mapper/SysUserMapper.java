package com.mido.pm.org.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.org.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** SysUser Mapper。 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 取指定租户最早的 active 用户 ID（模拟登录目标定位用）。
     * 跨租户访问：显式按 tenant_id 过滤并忽略多租户拦截器（否则会叠加当前上下文租户条件）。
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT id FROM sys_user WHERE tenant_id = #{tenantId} AND status = 'active' "
            + "AND is_deleted = 0 ORDER BY id LIMIT 1")
    Long selectPrimaryUserId(@Param("tenantId") Long tenantId);
}
