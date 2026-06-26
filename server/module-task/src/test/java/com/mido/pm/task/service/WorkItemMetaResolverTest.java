package com.mido.pm.task.service;

import com.mido.pm.task.entity.PmPriorityLevel;
import com.mido.pm.task.entity.PmPriorityMode;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.mapper.PmPriorityLevelMapper;
import com.mido.pm.task.mapper.PmPriorityModeMapper;
import com.mido.pm.task.mapper.PmStatusMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 工作项元数据解析器单测：解析命中返回 id；未种子返回 null（best-effort）。
 */
@ExtendWith(MockitoExtension.class)
class WorkItemMetaResolverTest {

    @Mock private PmWorkItemTypeMapper typeMapper;
    @Mock private PmStatusMapper statusMapper;
    @Mock private PmPriorityModeMapper priorityModeMapper;
    @Mock private PmPriorityLevelMapper priorityLevelMapper;

    private WorkItemMetaResolver resolver() {
        return new WorkItemMetaResolver(typeMapper, statusMapper, priorityModeMapper, priorityLevelMapper);
    }

    @Test
    void defaultTaskTypeIdReturnsNullWhenNotSeeded() {
        when(typeMapper.selectList(any())).thenReturn(List.of());
        assertNull(resolver().defaultTaskTypeId());
    }

    @Test
    void statusIdByNameResolves() {
        PmStatus s = new PmStatus();
        s.setId(2L);
        when(statusMapper.selectList(any())).thenReturn(List.of(s));
        assertEquals(2L, resolver().statusIdByName("进行中"));
    }

    @Test
    void priorityLevelIdResolvesViaBuiltinMode() {
        PmPriorityMode mode = new PmPriorityMode();
        mode.setId(1L);
        PmPriorityLevel level = new PmPriorityLevel();
        level.setId(3L);
        when(priorityModeMapper.selectList(any())).thenReturn(List.of(mode));
        when(priorityLevelMapper.selectList(any())).thenReturn(List.of(level));
        assertEquals(3L, resolver().priorityLevelId(2));
    }

    @Test
    void priorityLevelIdNullWhenNoBuiltinMode() {
        when(priorityModeMapper.selectList(any())).thenReturn(List.of());
        assertNull(resolver().priorityLevelId(2));
    }
}
