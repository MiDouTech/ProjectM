package com.mido.pm.org.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.org.entity.SysApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** SysApiKey Mapper。 */
@Mapper
public interface SysApiKeyMapper extends BaseMapper<SysApiKey> {

    /**
     * 按 key_hash 全局查找（鉴权用）。此时尚未知租户，须忽略多租户拦截器；
     * key 自身的 tenant_id/user_id 决定上下文。返回完整行（含 tenant_id、user_id、status、expire_at）。
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM sys_api_key WHERE key_hash = #{keyHash} AND is_deleted = 0 LIMIT 1")
    SysApiKey selectByKeyHash(@Param("keyHash") String keyHash);

    /** 鉴权成功后更新最近使用时间（忽略租户拦截器，按主键更新）。 */
    @InterceptorIgnore(tenantLine = "1")
    @org.apache.ibatis.annotations.Update("UPDATE sys_api_key SET last_used_at = NOW() WHERE id = #{id}")
    void touchLastUsed(@Param("id") Long id);
}
