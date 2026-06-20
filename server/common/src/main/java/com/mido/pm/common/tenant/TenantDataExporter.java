package com.mido.pm.common.tenant;

/**
 * 租户数据导出端口：各核心业务域导出「当前 TenantContext 租户」下的数据快照。
 * 平台导出任务设置租户上下文后聚合所有实现，序列化为 JSON 打包。实现分散在 org/project/task/goal。
 */
public interface TenantDataExporter {

    /** 导出域名（作为导出 JSON 的顶层 key，如 members/projects/tasks/goals）。 */
    String domain();

    /** 当前租户下该域的数据（查询经多租户拦截器按 TenantContext 隔离）。 */
    Object exportData();
}
