package com.mido.pm.org.service;

import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.org.dto.ApiKeyCreateDTO;
import com.mido.pm.org.dto.ApiKeyCreatedVO;
import com.mido.pm.org.entity.SysApiKey;
import com.mido.pm.org.mapper.SysApiKeyMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** API Key 服务单测：创建生成明文+哈希前缀；解析校验存在/状态/过期。 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private SysApiKeyMapper apiKeyMapper;
    @InjectMocks
    private ApiKeyService service;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private SysApiKey key(String status, LocalDateTime expireAt) {
        SysApiKey k = new SysApiKey();
        k.setId(1L);
        k.setTenantId(1L);
        k.setUserId(7L);
        k.setStatus(status);
        k.setExpireAt(expireAt);
        return k;
    }

    @Test
    void createGeneratesPlaintextAndPrefix() {
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        UserContext.set(u);

        ApiKeyCreatedVO vo = service.create(new ApiKeyCreateDTO("集成A", null, null));

        assertTrue(vo.apiKey().startsWith("mk_"), "明文应带 mk_ 前缀");
        assertEquals(11, vo.keyPrefix().length(), "前缀为 mk_ + 8 位");
        assertTrue(vo.apiKey().startsWith(vo.keyPrefix()));

        ArgumentCaptor<SysApiKey> captor = ArgumentCaptor.forClass(SysApiKey.class);
        verify(apiKeyMapper).insert(captor.capture());
        assertEquals("mcp:read,mcp:write", captor.getValue().getScopes(), "未指定 scopes 默认读写两档");
    }

    @Test
    void createHonorsExplicitReadOnlyScope() {
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        UserContext.set(u);

        service.create(new ApiKeyCreateDTO("只读连接器", null, "mcp:read"));

        ArgumentCaptor<SysApiKey> captor = ArgumentCaptor.forClass(SysApiKey.class);
        verify(apiKeyMapper).insert(captor.capture());
        assertEquals("mcp:read", captor.getValue().getScopes(), "应保留显式只读范围");
    }

    @Test
    void resolveRejectsNonPrefixed() {
        assertTrue(service.resolve("xyz").isEmpty());
    }

    @Test
    void resolveRejectsUnknown() {
        when(apiKeyMapper.selectByKeyHash(any())).thenReturn(null);
        assertTrue(service.resolve("mk_unknown").isEmpty());
    }

    @Test
    void resolveRejectsDisabled() {
        when(apiKeyMapper.selectByKeyHash(any())).thenReturn(key("disabled", null));
        assertTrue(service.resolve("mk_whatever").isEmpty());
    }

    @Test
    void resolveRejectsExpired() {
        when(apiKeyMapper.selectByKeyHash(any()))
                .thenReturn(key("active", LocalDateTime.now().minusDays(1)));
        assertTrue(service.resolve("mk_whatever").isEmpty());
    }

    @Test
    void resolveAcceptsValid() {
        lenient().when(apiKeyMapper.selectByKeyHash(any()))
                .thenReturn(key("active", LocalDateTime.now().plusDays(1)));
        assertTrue(service.resolve("mk_whatever").isPresent());
    }
}
