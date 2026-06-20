package com.mido.pm.goal.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 目标域物理清除（注销合规）。 */
@Mapper
public interface GoalPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_goal WHERE tenant_id = #{t}")
    int purgeGoals(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_goal_alignment WHERE tenant_id = #{t}")
    int purgeAlignments(@Param("t") Long tenantId);
}
