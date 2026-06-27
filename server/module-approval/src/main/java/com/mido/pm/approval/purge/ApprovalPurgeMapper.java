package com.mido.pm.approval.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 审批域物理清除（注销合规）。表名无 pm_ 前缀。任务→实例→表单/流程顺序。 */
@Mapper
public interface ApprovalPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM approval_task WHERE tenant_id = #{t}")
    int purgeTasks(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM approval_instance WHERE tenant_id = #{t}")
    int purgeInstances(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM approval_form WHERE tenant_id = #{t}")
    int purgeForms(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM approval_flow WHERE tenant_id = #{t}")
    int purgeFlows(@Param("t") Long tenantId);
}
