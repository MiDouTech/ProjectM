package com.mido.pm.common.quota;

/**
 * 配额校验端口：业务域在创建资源前调用，按当前租户订阅套餐的配额硬校验。
 * 实现在 platform（读当前 TenantContext 租户 → 生效订阅 → 套餐配额）；
 * 业务域只依赖本接口，避免反向依赖 module-platform。
 */
public interface QuotaGuard {

    /**
     * 校验当前租户某资源在新增一个后是否仍在配额内，超限抛 BizException。
     * 配额上限为 -1（不限）或租户未订阅时直接放行。
     *
     * @param resource     资源标识（如 user/project，见 QuotaResources）
     * @param currentCount 该资源当前已有数量（业务域提供）
     */
    void checkCanAdd(String resource, long currentCount);
}
