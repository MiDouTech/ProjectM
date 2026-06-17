package com.mido.pm.provider.identity;

import java.util.Optional;

/**
 * 企微身份 Provider（TODO·P2 激活）：通讯录同步 + sys_identity_map 映射。
 * 阶段一不注册为 Bean，占位预留。
 */
public class WecomIdentityProvider implements IdentityProvider {

    @Override
    public Optional<UserPrincipal> loadByUsername(String username) {
        throw new UnsupportedOperationException("TODO: 企微通讯录身份实现，P2 激活");
    }

    @Override
    public Optional<UserPrincipal> loadById(Long userId) {
        throw new UnsupportedOperationException("TODO: 企微通讯录身份实现，P2 激活");
    }
}
