package com.mido.pm.provider.identity;

/**
 * 身份/组织 Provider 接口。屏蔽底层身份源（本地库 / 企微组织）。
 * 业务层只依赖本接口，禁止直接 import 企微 SDK。
 */
public interface IdentityProvider {

    /** 按用户 ID 取显示名。 */
    String getUserName(Long userId);

    /** 用户是否有效（在职/启用）。 */
    boolean isActive(Long userId);
}
