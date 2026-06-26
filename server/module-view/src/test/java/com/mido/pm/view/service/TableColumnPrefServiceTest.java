package com.mido.pm.view.service;

import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 表头偏好服务单测：无记录则 insert、有记录则 update；非法 listKey 拒绝。
 */
@ExtendWith(MockitoExtension.class)
class TableColumnPrefServiceTest {

    @Mock
    private PmViewMapper viewMapper;

    private TableColumnPrefService service() {
        return new TableColumnPrefService(viewMapper);
    }

    private final Map<String, Object> cfg = Map.of("columns", List.of("name", "status"), "frozen", List.of("name"));

    @Test
    void save_noExisting_inserts() {
        when(viewMapper.selectOne(any())).thenReturn(null);
        service().save("projects", cfg);
        verify(viewMapper).insert(any(PmView.class));
        verify(viewMapper, never()).updateById(any(PmView.class));
    }

    @Test
    void save_existing_updates() {
        PmView existing = new PmView();
        existing.setId(1L);
        when(viewMapper.selectOne(any())).thenReturn(existing);
        service().save("tasks", cfg);
        verify(viewMapper).updateById(any(PmView.class));
        verify(viewMapper, never()).insert(any(PmView.class));
    }

    @Test
    void save_illegalListKey_rejected() {
        assertThrows(RuntimeException.class, () -> service().save("BAD KEY!", cfg));
        verify(viewMapper, never()).insert(any(PmView.class));
    }
}
