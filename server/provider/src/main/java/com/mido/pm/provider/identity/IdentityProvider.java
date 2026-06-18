package com.mido.pm.provider.identity;

import java.util.Optional;

/**
 * 身份/组织 Provider 接口：查用户/部门/职级、装配权限与数据范围。
 * 屏蔽底层身份源（本地库 / 企微组织）。业务层只依赖本接口，禁止直接 import 企微 SDK。
 *
 * <p>本地实现 {@code OrgIdentityProvider} 位于 module-org（因需读 sys_*，为不让 provider 依赖业务模块
 * 而下沉到 org）；企微实现 {@link WecomIdentityProvider} 预留。</p>
 */
public interface IdentityProvider {

    /** 按用户名加载（含密码哈希，供登录校验）。 */
    Optional<UserPrincipal> loadByUsername(String username);

    /** 按手机号加载（含密码哈希，供手机号登录校验）。 */
    Optional<UserPrincipal> loadByPhone(String phone);

    /** 按用户 ID 加载（含权限码/数据范围，供令牌校验后装配安全上下文）。 */
    Optional<UserPrincipal> loadById(Long userId);
}
