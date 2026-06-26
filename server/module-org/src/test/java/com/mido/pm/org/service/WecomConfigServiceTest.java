package com.mido.pm.org.service;

import com.mido.pm.common.security.SecretCipher;
import com.mido.pm.org.dto.WecomConfigSaveDTO;
import com.mido.pm.org.dto.WecomConfigStatusVO;
import com.mido.pm.org.dto.WecomConfigVO;
import com.mido.pm.org.entity.PmWecomConfig;
import com.mido.pm.org.mapper.PmWecomConfigMapper;
import com.mido.pm.provider.identity.WecomContactClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 企微配置服务单测：secret 加密入库 / 脱敏出参 / 留空保持原值 / 启用状态 / 同步凭证解密。 */
class WecomConfigServiceTest {

    private final SecretCipher cipher = new SecretCipher("unit-test-key");

    private WecomConfigService service(PmWecomConfigMapper mapper, WecomContactClient client) {
        return new WecomConfigService(mapper, cipher, client);
    }

    @Test
    void getReturnsEmptyWhenNoRow() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        WecomConfigVO vo = service(mapper, mock(WecomContactClient.class)).get();
        assertNull(vo.corpId());
        assertFalse(vo.contactsEnabled());
        assertFalse(vo.contactsSecretSet());
    }

    @Test
    void saveEncryptsSecretAndNeverStoresPlaintext() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        service(mapper, mock(WecomContactClient.class))
                .save(new WecomConfigSaveDTO("ww123", true, "plain-secret", false, null, null, false, null, null));

        ArgumentCaptor<PmWecomConfig> cap = ArgumentCaptor.forClass(PmWecomConfig.class);
        verify(mapper).insert(cap.capture());
        PmWecomConfig saved = cap.getValue();
        assertEquals("ww123", saved.getCorpId());
        assertEquals(1, saved.getContactsEnabled());
        assertNotNull(saved.getContactsSecret());
        assertFalse(saved.getContactsSecret().contains("plain-secret"));
        assertEquals("plain-secret", cipher.decrypt(saved.getContactsSecret()));
    }

    @Test
    void saveWithBlankSecretKeepsExisting() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        PmWecomConfig existing = new PmWecomConfig();
        existing.setCorpId("ww123");
        existing.setContactsEnabled(1);
        existing.setContactsSecret(cipher.encrypt("old-secret"));
        when(mapper.selectOne(any())).thenReturn(existing);

        service(mapper, mock(WecomContactClient.class))
                .save(new WecomConfigSaveDTO("ww123", true, "", false, null, null, false, null, null));

        ArgumentCaptor<PmWecomConfig> cap = ArgumentCaptor.forClass(PmWecomConfig.class);
        verify(mapper).updateById(cap.capture());
        verify(mapper, never()).insert(any(PmWecomConfig.class));
        assertEquals("old-secret", cipher.decrypt(cap.getValue().getContactsSecret()));
    }

    @Test
    void statusEnabledWhenDbConfigured() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        PmWecomConfig c = new PmWecomConfig();
        c.setCorpId("ww123");
        c.setContactsEnabled(1);
        c.setContactsSecret(cipher.encrypt("s"));
        when(mapper.selectOne(any())).thenReturn(c);
        WecomContactClient client = mock(WecomContactClient.class);
        when(client.enabled()).thenReturn(false);

        WecomConfigStatusVO st = service(mapper, client).status();
        assertTrue(st.contactsEnabled());
        assertFalse(st.ssoEnabled());
    }

    @Test
    void findEnabledContactsCredsDecrypts() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        PmWecomConfig c = new PmWecomConfig();
        c.setCorpId("ww123");
        c.setContactsEnabled(1);
        c.setContactsSecret(cipher.encrypt("real-secret"));
        when(mapper.selectOne(any())).thenReturn(c);

        WecomConfigService.ContactsCreds creds = service(mapper, mock(WecomContactClient.class)).findEnabledContactsCreds();
        assertNotNull(creds);
        assertEquals("ww123", creds.corpId());
        assertEquals("real-secret", creds.secret());
    }

    @Test
    void findEnabledContactsCredsNullWhenDisabled() {
        PmWecomConfigMapper mapper = mock(PmWecomConfigMapper.class);
        PmWecomConfig c = new PmWecomConfig();
        c.setCorpId("ww123");
        c.setContactsEnabled(0);
        c.setContactsSecret(cipher.encrypt("s"));
        when(mapper.selectOne(any())).thenReturn(c);
        assertNull(service(mapper, mock(WecomContactClient.class)).findEnabledContactsCreds());
    }
}
