package com.mido.pm.report.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 报表(PMO度量配置)域物理清除（注销合规）。 */
@Mapper
public interface ReportPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_report_setting WHERE tenant_id = #{t}")
    int purgeSettings(@Param("t") Long tenantId);
}
