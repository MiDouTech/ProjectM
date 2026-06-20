package com.mido.pm.org.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.org.dto.ApiKeyCreateDTO;
import com.mido.pm.org.dto.ApiKeyCreatedVO;
import com.mido.pm.org.dto.ApiKeyVO;
import com.mido.pm.org.entity.SysApiKey;
import com.mido.pm.org.mapper.SysApiKeyMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 开放平台 API Key 服务：当前租户内的 key 管理（绑定登录用户）+ 鉴权解析。
 * 明文 key 仅创建时返回一次，库内只存 SHA-256 与前缀。
 */
@Service
public class ApiKeyService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";
    private static final String PREFIX = "mk_";

    private final SysApiKeyMapper apiKeyMapper;

    public ApiKeyService(SysApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    public List<ApiKeyVO> list() {
        return apiKeyMapper.selectList(Wrappers.<SysApiKey>lambdaQuery().orderByDesc(SysApiKey::getId))
                .stream().map(this::toVO).toList();
    }

    /** 创建 key，绑定当前登录用户；返回明文（仅此一次）。 */
    public ApiKeyCreatedVO create(ApiKeyCreateDTO dto) {
        Long userId = UserContext.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String raw = PREFIX + RandomUtil.randomString(40);
        SysApiKey entity = new SysApiKey();
        entity.setUserId(userId);
        entity.setName(dto.name());
        entity.setKeyHash(DigestUtil.sha256Hex(raw));
        entity.setKeyPrefix(raw.substring(0, 11));
        entity.setStatus(STATUS_ACTIVE);
        entity.setExpireAt(dto.expireAt());
        apiKeyMapper.insert(entity);
        return new ApiKeyCreatedVO(entity.getId(), entity.getName(), raw, entity.getKeyPrefix());
    }

    /** 停用 key。 */
    public void revoke(Long id) {
        SysApiKey k = requireExists(id);
        k.setStatus(STATUS_DISABLED);
        apiKeyMapper.updateById(k);
    }

    /** 删除 key（逻辑删除）。 */
    public void delete(Long id) {
        requireExists(id);
        apiKeyMapper.deleteById(id);
    }

    /**
     * 鉴权解析：按明文 key 找到有效（存在/active/未过期）的 key 行；否则空。
     * 不走租户隔离（key 自身决定租户）。
     */
    public Optional<SysApiKey> resolve(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(PREFIX)) {
            return Optional.empty();
        }
        SysApiKey k = apiKeyMapper.selectByKeyHash(DigestUtil.sha256Hex(rawKey));
        if (k == null || STATUS_DISABLED.equalsIgnoreCase(k.getStatus())) {
            return Optional.empty();
        }
        if (k.getExpireAt() != null && k.getExpireAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(k);
    }

    /** 更新最近使用时间（鉴权成功后调用）。 */
    public void touch(Long id) {
        apiKeyMapper.touchLastUsed(id);
    }

    private SysApiKey requireExists(Long id) {
        SysApiKey k = apiKeyMapper.selectById(id);
        if (k == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "API Key 不存在");
        }
        return k;
    }

    private ApiKeyVO toVO(SysApiKey k) {
        return new ApiKeyVO(k.getId(), k.getName(), k.getKeyPrefix(), k.getStatus(), k.getUserId(),
                k.getLastUsedAt(), k.getExpireAt(), k.getCreateTime());
    }
}
