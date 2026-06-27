package com.mido.pm.view.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.view.entity.PmPageConfig;
import com.mido.pm.view.mapper.PmPageConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 页面配置服务单测：内置字段目录、配置 upsert、非法 target/模板拒绝。
 */
@ExtendWith(MockitoExtension.class)
class PageConfigServiceTest {

    @Mock
    private PmPageConfigMapper mapper;

    private PageConfigService service() {
        return new PageConfigService(mapper);
    }

    @Test
    void builtinFields_task_hasTitleRequired() {
        var fields = service().builtinFields("task");
        assertTrue(fields.stream().anyMatch(f -> "title".equals(f.key()) && f.required()));
    }

    @Test
    void builtinFields_unknownTarget_rejected() {
        assertThrows(BizException.class, () -> service().builtinFields("ghost"));
    }

    @Test
    void save_noExisting_inserts() {
        when(mapper.selectOne(any())).thenReturn(null);
        service().save("task", "form", Map.of("fields", List.of()));
        verify(mapper).insert(any(PmPageConfig.class));
        verify(mapper, never()).updateById(any(PmPageConfig.class));
    }

    @Test
    void save_existing_updates() {
        PmPageConfig existing = new PmPageConfig();
        existing.setId(1L);
        when(mapper.selectOne(any())).thenReturn(existing);
        service().save("task", "form", Map.of("fields", List.of()));
        verify(mapper).updateById(any(PmPageConfig.class));
        verify(mapper, never()).insert(any(PmPageConfig.class));
    }

    @Test
    void save_unknownTemplate_rejected() {
        assertThrows(BizException.class, () -> service().save("task", "wizard", Map.of()));
        verify(mapper, never()).insert(any(PmPageConfig.class));
    }

    @Test
    void get_noConfig_returnsNull() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertEquals(null, service().get("task", "form"));
    }
}
