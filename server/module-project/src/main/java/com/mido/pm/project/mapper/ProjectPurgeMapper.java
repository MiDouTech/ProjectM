package com.mido.pm.project.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 项目域物理清除（注销合规）。 */
@Mapper
public interface ProjectPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_project WHERE tenant_id = #{t}")
    int purgeProjects(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_project_member WHERE tenant_id = #{t}")
    int purgeMembers(@Param("t") Long tenantId);
}
