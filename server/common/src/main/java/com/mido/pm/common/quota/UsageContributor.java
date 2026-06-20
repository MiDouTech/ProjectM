package com.mido.pm.common.quota;

/**
 * 用量贡献端口：各业务域上报某资源在「当前 TenantContext 租户」下的实时数量。
 * 平台用量快照任务设置租户上下文后，聚合所有实现。实现分散在 org/project/task/collab。
 */
public interface UsageContributor {

    /** 贡献的资源标识（QuotaResources.*）。 */
    String resource();

    /** 当前租户下该资源的数量（查询经多租户拦截器按当前 TenantContext 隔离）。 */
    long currentCount();
}
