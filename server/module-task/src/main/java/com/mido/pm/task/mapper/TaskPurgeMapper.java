package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 任务域物理清除（注销合规）。 */
@Mapper
public interface TaskPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_task WHERE tenant_id = #{t}")
    int purgeTasks(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_task_dependency WHERE tenant_id = #{t}")
    int purgeDependencies(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_work_hour WHERE tenant_id = #{t}")
    int purgeWorkHours(@Param("t") Long tenantId);
}
