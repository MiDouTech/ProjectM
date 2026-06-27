package com.mido.pm.field.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 自定义字段/数据源域物理清除（注销合规）。值/选项先于定义/数据源。 */
@Mapper
public interface FieldPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_field_value WHERE tenant_id = #{t}")
    int purgeFieldValues(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_data_source_option WHERE tenant_id = #{t}")
    int purgeDataSourceOptions(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_data_source WHERE tenant_id = #{t}")
    int purgeDataSources(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_field_def WHERE tenant_id = #{t}")
    int purgeFieldDefs(@Param("t") Long tenantId);
}
