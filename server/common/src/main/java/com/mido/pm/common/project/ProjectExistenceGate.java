package com.mido.pm.common.project;

/**
 * 项目存在性闸门：跨域校验某 projectId 在当前租户下确实存在。
 * 端口定义在 common，由 module-project 实现，其他域（如 module-verify 录入结果验收前）消费，
 * 跨域不成环（沿用 ResultVerifyGate 同模式）。projectId 不存在/不属当前租户时实现方抛 BizException。
 */
public interface ProjectExistenceGate {

    /** 项目不存在（或不属当前租户）则抛 BizException，阻断后续写入。 */
    void assertExists(Long projectId);
}
