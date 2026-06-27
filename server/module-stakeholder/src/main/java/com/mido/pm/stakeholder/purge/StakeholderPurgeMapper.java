package com.mido.pm.stakeholder.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 干系人域物理清除（注销合规）。 */
@Mapper
public interface StakeholderPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_stakeholder WHERE tenant_id = #{t}")
    int purgeStakeholders(@Param("t") Long tenantId);
}
