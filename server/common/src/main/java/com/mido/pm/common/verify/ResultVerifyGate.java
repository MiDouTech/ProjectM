package com.mido.pm.common.verify;

/**
 * 结果验收闸门（铁三角）：项目「结果验收 → 已结案」前置硬校验。
 * 端口定义在 common，由 module-verify 实现，module-project 在流转时消费，跨域不成环
 * （沿用 QuotaGuard 同模式）。无「达标(pass)」结论时实现方抛 BizException 拦截结案。
 */
public interface ResultVerifyGate {

    /** 无达标的结果验收结论则抛 BizException，阻断结案。 */
    void assertClosable(Long projectId);
}
