package com.mido.pm.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报表只读聚合（读模型，阶段一直查 pm_task/pm_project；tenant 与数据范围由全局拦截器注入 WHERE）。
 * 逻辑删 is_deleted 需手写（@Select 不走 @TableLogic）。后续可整体切 ES，本 Mapper 即替换点，上层契约不变。
 */
@Mapper
public interface ReportMapper {

    String TASK_AGG = "COUNT(*) total, "
            + "SUM(CASE WHEN status IN ('已完成','已验收') THEN 1 ELSE 0 END) completed, "
            + "SUM(CASE WHEN due_date IS NOT NULL AND due_date < CURDATE() "
            + "         AND status NOT IN ('已完成','已验收') THEN 1 ELSE 0 END) overdue";

    @Select("SELECT " + TASK_AGG + " FROM pm_task WHERE is_deleted = 0")
    Map<String, Object> taskCounts();

    @Select("SELECT " + TASK_AGG + " FROM pm_task WHERE is_deleted = 0 AND project_id = #{projectId}")
    Map<String, Object> taskCountsByProject(@Param("projectId") Long projectId);

    @Select("SELECT category, COUNT(*) cnt FROM pm_project WHERE is_deleted = 0 GROUP BY category")
    List<Map<String, Object>> categoryDistribution();

    @Select("SELECT budget, actual_cost actualCost FROM pm_project WHERE id = #{projectId} AND is_deleted = 0")
    Map<String, Object> projectBudget(@Param("projectId") Long projectId);

    @Select("SELECT due_date dueDate, COUNT(*) cnt FROM pm_task "
            + "WHERE is_deleted = 0 AND project_id = #{projectId} AND due_date IS NOT NULL "
            + "GROUP BY due_date ORDER BY due_date")
    List<Map<String, Object>> dueBuckets(@Param("projectId") Long projectId);
}
