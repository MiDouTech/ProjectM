package com.mido.pm.cost.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 费用域物理清除（注销合规）。 */
@Mapper
public interface CostPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_cost WHERE tenant_id = #{t}")
    int purgeCosts(@Param("t") Long tenantId);
}
