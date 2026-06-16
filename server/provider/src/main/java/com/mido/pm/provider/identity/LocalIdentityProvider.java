package com.mido.pm.provider.identity;

import org.springframework.stereotype.Component;

/**
 * 本地身份 Provider 占位实现。阶段一基于本地库，企微实现（WecomIdentityProvider）预留。
 */
@Component
public class LocalIdentityProvider implements IdentityProvider {

    @Override
    public String getUserName(Long userId) {
        // TODO 阶段二接入 sys_user 查询
        return userId == null ? null : ("user-" + userId);
    }

    @Override
    public boolean isActive(Long userId) {
        return userId != null;
    }
}
