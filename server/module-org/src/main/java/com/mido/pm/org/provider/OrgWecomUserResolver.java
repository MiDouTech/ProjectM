package com.mido.pm.org.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.org.entity.SysIdentityMap;
import com.mido.pm.org.mapper.SysIdentityMapMapper;
import com.mido.pm.provider.message.WecomUserResolver;
import org.springframework.stereotype.Component;

/**
 * 企微用户解析（org 侧实现）：查 sys_identity_map(provider='wecom') 取本地 userId → 企微 userid。
 */
@Component
public class OrgWecomUserResolver implements WecomUserResolver {

    private static final String PROVIDER_WECOM = "wecom";

    private final SysIdentityMapMapper identityMapMapper;

    public OrgWecomUserResolver(SysIdentityMapMapper identityMapMapper) {
        this.identityMapMapper = identityMapMapper;
    }

    @Override
    public String externalUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SysIdentityMap map = identityMapMapper.selectOne(Wrappers.<SysIdentityMap>lambdaQuery()
                .eq(SysIdentityMap::getUserId, userId)
                .eq(SysIdentityMap::getProvider, PROVIDER_WECOM)
                .last("limit 1"));
        return map == null ? null : map.getExternalId();
    }
}
