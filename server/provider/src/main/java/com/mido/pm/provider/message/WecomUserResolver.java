package com.mido.pm.provider.message;

/**
 * 企微用户解析端口：本地 userId → 企微 userid。
 *
 * <p>provider 层不依赖业务模块（module-org 反向依赖 provider），映射数据在 {@code sys_identity_map}，
 * 故以端口下沉、由 module-org 实现注入。无映射返回 {@code null}（视为该用户未绑定企微，跳过外呼）。</p>
 */
public interface WecomUserResolver {

    /** 本地 userId 对应的企微 userid；未绑定返回 null。 */
    String externalUserId(Long userId);
}
