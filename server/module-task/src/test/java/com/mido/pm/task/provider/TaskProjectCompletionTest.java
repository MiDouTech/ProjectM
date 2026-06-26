package com.mido.pm.task.provider;

import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.task.service.WorkItemMetaResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 项目完成率单测：按状态库元类别(doneIds)统计完成率；总数为 0 返回 0。
 */
@ExtendWith(MockitoExtension.class)
class TaskProjectCompletionTest {

    @Mock private PmTaskMapper taskMapper;
    @Mock private WorkItemMetaResolver metaResolver;

    private TaskProjectCompletion port() {
        return new TaskProjectCompletion(taskMapper, metaResolver);
    }

    @Test
    void completionRateByMetaDone() {
        // 总数 4，完成 2 → 50.00
        when(taskMapper.selectCount(any())).thenReturn(4L, 2L);
        lenient().when(metaResolver.doneStatusIds()).thenReturn(List.of(3L, 4L));
        assertEquals(0, new BigDecimal("50.00").compareTo(port().completionRate(1L)));
    }

    @Test
    void zeroWhenNoTasks() {
        when(taskMapper.selectCount(any())).thenReturn(0L);
        assertEquals(0, BigDecimal.ZERO.compareTo(port().completionRate(1L)));
    }
}
