package com.mido.pm.change.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 变更中心域物理清除（注销合规）。 */
@Mapper
public interface ChangePurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_change_request WHERE tenant_id = #{t}")
    int purgeRequests(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_change_policy WHERE tenant_id = #{t}")
    int purgePolicies(@Param("t") Long tenantId);
}
