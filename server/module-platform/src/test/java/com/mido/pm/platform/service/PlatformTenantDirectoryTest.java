package com.mido.pm.platform.service;

import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** 租户可登录判定单测：状态 + 到期日期（P0-b）。 */
@ExtendWith(MockitoExtension.class)
class PlatformTenantDirectoryTest {

    @Mock
    private SysTenantMapper tenantMapper;

    @InjectMocks
    private PlatformTenantDirectory directory;

    private SysTenant tenant(String status, LocalDateTime expireAt) {
        SysTenant t = new SysTenant();
        t.setId(9L);
        t.setStatus(status);
        t.setExpireAt(expireAt);
        return t;
    }

    @Test
    void activeWithoutExpireIsLoginable() {
        when(tenantMapper.selectById(9L)).thenReturn(tenant("active", null));
        assertTrue(directory.isLoginable(9L));
    }

    @Test
    void activeWithFutureExpireIsLoginable() {
        when(tenantMapper.selectById(9L)).thenReturn(tenant("active", LocalDateTime.now().plusDays(3)));
        assertTrue(directory.isLoginable(9L));
    }

    @Test
    void activeButOverdueIsNotLoginable() {
        when(tenantMapper.selectById(9L)).thenReturn(tenant("active", LocalDateTime.now().minusMinutes(1)));
        assertFalse(directory.isLoginable(9L));
    }

    @Test
    void suspendedIsNotLoginable() {
        when(tenantMapper.selectById(9L)).thenReturn(tenant("suspended", null));
        assertFalse(directory.isLoginable(9L));
    }

    @Test
    void nullTenantIdIsNotLoginable() {
        assertFalse(directory.isLoginable(null));
    }
}
