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

    // 翻转读方：完成判定优先按状态库「元类别=已完成」(doneCsv=该元类别状态 id 列表)；
    // status_id 为空的存量行 / 未配置状态库的租户(doneCsv 为空)回落字符串终态(已完成/已验收)。
    String DONE_COND = "( (#{doneCsv} <> '' AND FIND_IN_SET(status_id, #{doneCsv}) > 0) "
            + "OR (status_id IS NULL AND status IN ('已完成','已验收')) )";

    String TASK_AGG = "COUNT(*) total, "
            + "SUM(CASE WHEN " + DONE_COND + " THEN 1 ELSE 0 END) completed, "
            + "SUM(CASE WHEN due_date IS NOT NULL AND due_date < CURDATE() "
            + "         AND NOT " + DONE_COND + " THEN 1 ELSE 0 END) overdue";

    @Select("SELECT " + TASK_AGG + " FROM pm_task WHERE is_deleted = 0")
    Map<String, Object> taskCounts(@Param("doneCsv") String doneCsv);

    @Select("SELECT " + TASK_AGG + " FROM pm_task WHERE is_deleted = 0 AND project_id = #{projectId}")
    Map<String, Object> taskCountsByProject(@Param("projectId") Long projectId, @Param("doneCsv") String doneCsv);

    @Select("SELECT category, COUNT(*) cnt FROM pm_project WHERE is_deleted = 0 GROUP BY category")
    List<Map<String, Object>> categoryDistribution();

    @Select("SELECT budget, actual_cost actualCost FROM pm_project WHERE id = #{projectId} AND is_deleted = 0")
    Map<String, Object> projectBudget(@Param("projectId") Long projectId);

    @Select("SELECT due_date dueDate, COUNT(*) cnt FROM pm_task "
            + "WHERE is_deleted = 0 AND project_id = #{projectId} AND due_date IS NOT NULL "
            + "GROUP BY due_date ORDER BY due_date")
    List<Map<String, Object>> dueBuckets(@Param("projectId") Long projectId);

    /** 人员负荷：按负责人聚合未完成(进行中)任务数与其中逾期数；仅保留有在办任务的人，负荷降序。 */
    @Select("SELECT assignee_id assigneeId, "
            + "SUM(CASE WHEN NOT " + DONE_COND + " THEN 1 ELSE 0 END) inProgress, "
            + "SUM(CASE WHEN due_date IS NOT NULL AND due_date < CURDATE() "
            + "         AND NOT " + DONE_COND + " THEN 1 ELSE 0 END) overdue "
            + "FROM pm_task WHERE is_deleted = 0 AND assignee_id IS NOT NULL "
            + "GROUP BY assignee_id HAVING inProgress > 0 ORDER BY inProgress DESC")
    List<Map<String, Object>> workloadByAssignee(@Param("doneCsv") String doneCsv);
}
