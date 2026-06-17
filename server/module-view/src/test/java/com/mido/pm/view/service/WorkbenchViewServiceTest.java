package com.mido.pm.view.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.view.dto.WorkbenchLayoutDTO;
import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作台布局服务单测（mock mapper，无 DB）：
 * 无记录则插入、有记录则更新（upsert），读取按 owner+scope 命中并解析卡片顺序。
 */
@ExtendWith(MockitoExtension.class)
class WorkbenchViewServiceTest {

    @Mock private PmViewMapper viewMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WorkbenchViewService service;

    @BeforeEach
    void setUp() {
        service = new WorkbenchViewService(viewMapper, objectMapper);
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void saveInsertsWhenNoExisting() {
        when(viewMapper.selectOne(any())).thenReturn(null);

        service.saveMyLayout(new WorkbenchLayoutDTO(List.of("myTasks", "myProjects")));

        ArgumentCaptor<PmView> captor = ArgumentCaptor.forClass(PmView.class);
        verify(viewMapper).insert(captor.capture());
        verify(viewMapper, never()).updateById(any(PmView.class));
        PmView saved = captor.getValue();
        assertEquals("workbench", saved.getScope());
        assertEquals(7L, saved.getOwnerId());
        assertEquals(true, saved.getConfig().contains("myTasks"));
    }

    @Test
    void saveUpdatesWhenExisting() {
        PmView existing = new PmView();
        existing.setId(100L);
        existing.setScope("workbench");
        existing.setOwnerId(7L);
        existing.setConfig("[\"old\"]");
        when(viewMapper.selectOne(any())).thenReturn(existing);

        service.saveMyLayout(new WorkbenchLayoutDTO(List.of("myProjects")));

        verify(viewMapper).updateById(existing);
        verify(viewMapper, never()).insert(any(PmView.class));
        assertEquals(true, existing.getConfig().contains("myProjects"));
    }

    @Test
    void getReturnsNullCardsWhenNoneSaved() {
        when(viewMapper.selectOne(any())).thenReturn(null);
        assertEquals(null, service.getMyLayout().cards());
    }

    @Test
    void getParsesStoredCardOrder() {
        PmView existing = new PmView();
        existing.setConfig("[\"a\",\"b\",\"c\"]");
        when(viewMapper.selectOne(any())).thenReturn(existing);
        assertEquals(List.of("a", "b", "c"), service.getMyLayout().cards());
    }
}
