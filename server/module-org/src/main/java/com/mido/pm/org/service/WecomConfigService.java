package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.security.SecretCipher;
import com.mido.pm.org.dto.WecomConfigSaveDTO;
import com.mido.pm.org.dto.WecomConfigStatusVO;
import com.mido.pm.org.dto.WecomConfigVO;
import com.mido.pm.org.entity.PmWecomConfig;
import com.mido.pm.org.mapper.PmWecomConfigMapper;
import com.mido.pm.provider.identity.WecomContactClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 企业微信集成配置服务：租户自助维护企微凭证与开关（取代「只能改环境变量」）。
 *
 * <p>secret 一律 AES 加密入库、脱敏出接口；通讯录同步的有效凭证优先取 DB 配置，回落环境变量。</p>
 */
@Service
public class WecomConfigService {

    private final PmWecomConfigMapper mapper;
    private final SecretCipher cipher;
    private final WecomContactClient contactClient;

    public WecomConfigService(PmWecomConfigMapper mapper, SecretCipher cipher, WecomContactClient contactClient) {
        this.mapper = mapper;
        this.cipher = cipher;
        this.contactClient = contactClient;
    }

    /** 当前租户配置（脱敏）。无配置时返回全空/全关。 */
    public WecomConfigVO get() {
        PmWecomConfig c = current();
        if (c == null) {
            return new WecomConfigVO(null, false, false, false, null, false, false, null, false, null, null);
        }
        return new WecomConfigVO(
                c.getCorpId(),
                isOn(c.getContactsEnabled()), notBlank(c.getContactsSecret()),
                isOn(c.getSsoEnabled()), c.getSsoAgentId(), notBlank(c.getSsoSecret()),
                isOn(c.getMsgEnabled()), c.getMsgAgentId(), notBlank(c.getMsgSecret()),
                c.getLastSyncAt(), c.getLastSyncResult());
    }

    /** 保存配置：secret 传空表示保持原值不变；非空则加密更新。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(WecomConfigSaveDTO dto) {
        PmWecomConfig c = current();
        boolean isNew = (c == null);
        if (isNew) {
            c = new PmWecomConfig();
        }
        c.setCorpId(trimToNull(dto.corpId()));
        c.setContactsEnabled(toFlag(dto.contactsEnabled(), c.getContactsEnabled()));
        c.setSsoEnabled(toFlag(dto.ssoEnabled(), c.getSsoEnabled()));
        c.setSsoAgentId(trimToNull(dto.ssoAgentId()));
        c.setMsgEnabled(toFlag(dto.msgEnabled(), c.getMsgEnabled()));
        c.setMsgAgentId(trimToNull(dto.msgAgentId()));
        c.setContactsSecret(mergeSecret(dto.contactsSecret(), c.getContactsSecret()));
        c.setSsoSecret(mergeSecret(dto.ssoSecret(), c.getSsoSecret()));
        c.setMsgSecret(mergeSecret(dto.msgSecret(), c.getMsgSecret()));
        if (isNew) {
            mapper.insert(c);
        } else {
            mapper.updateById(c);
        }
    }

    /** 各能力启用状态（DB 配置 + 环境变量综合）。 */
    public WecomConfigStatusVO status() {
        PmWecomConfig c = current();
        boolean contacts = enabledContactsCreds(c) != null || contactClient.enabled();
        boolean sso = c != null && isOn(c.getSsoEnabled()) && notBlank(c.getCorpId()) && notBlank(c.getSsoSecret());
        boolean msg = c != null && isOn(c.getMsgEnabled()) && notBlank(c.getCorpId()) && notBlank(c.getMsgSecret());
        return new WecomConfigStatusVO(contacts, sso, msg);
    }

    /** 通讯录同步可用凭证（DB 配置，已解密）；不可用返回 null。供 {@link WecomContactSyncService}。 */
    public ContactsCreds findEnabledContactsCreds() {
        return enabledContactsCreds(current());
    }

    /** 记录最近一次同步结果（用于配置页展示）。 */
    @Transactional(rollbackFor = Exception.class)
    public void recordSync(String result) {
        PmWecomConfig c = current();
        if (c == null) {
            return;
        }
        c.setLastSyncAt(LocalDateTime.now());
        c.setLastSyncResult(result);
        mapper.updateById(c);
    }

    private ContactsCreds enabledContactsCreds(PmWecomConfig c) {
        if (c != null && isOn(c.getContactsEnabled()) && notBlank(c.getCorpId()) && notBlank(c.getContactsSecret())) {
            return new ContactsCreds(c.getCorpId(), cipher.decrypt(c.getContactsSecret()));
        }
        return null;
    }

    private PmWecomConfig current() {
        return mapper.selectOne(Wrappers.<PmWecomConfig>lambdaQuery().last("LIMIT 1"));
    }

    /** secret 合并策略：入参非空→加密为新值；入参空→保持原密文。 */
    private String mergeSecret(String input, String existingCipher) {
        return (input == null || input.isBlank()) ? existingCipher : cipher.encrypt(input.trim());
    }

    private Integer toFlag(Boolean input, Integer existing) {
        if (input != null) {
            return input ? 1 : 0;
        }
        return existing == null ? 0 : existing;
    }

    private static boolean isOn(Integer v) {
        return v != null && v == 1;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static String trimToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 通讯录有效凭证（明文，仅内部外呼用）。 */
    public record ContactsCreds(String corpId, String secret) {
    }
}
