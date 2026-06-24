package com.mido.pm.task.service;

import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.dto.TaskChangeRequestDTO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 重大任务变更发起单测：组装 after（含负责人/截止日）并委托变更中心；非法类型/无变更/起止逆序拒绝。 */
@ExtendWith(MockitoExtension.class)
class TaskChangeServiceTest {

    @Mock
    private PmTaskMapper taskMapper;
    @Mock
    private ChangeService changeService;
    @InjectMocks
    private TaskChangeService service;

    private PmTask task() {
        PmTask t = new PmTask();
        t.setId(1L);
        t.setTitle("任务A");
        t.setStartDate(LocalDate.of(2026, 6, 1));
        t.setDueDate(LocalDate.of(2026, 6, 10));
        t.setAssigneeId(100L);
        return t;
    }

    @Test
    void submitBuildsAfterAndDelegates() {
        when(taskMapper.selectById(1L)).thenReturn(task());
        when(changeService.submit(any())).thenReturn(9L);

        Long id = service.submit(1L, new TaskChangeRequestDTO(
                "task_baseline", "改派并顺延", null, null, LocalDate.of(2026, 6, 20), 200L));

        assertEquals(9L, id);
        ArgumentCaptor<ChangeSubmitCmd> cmd = ArgumentCaptor.forClass(ChangeSubmitCmd.class);
        verify(changeService).submit(cmd.capture());
        assertEquals("task", cmd.getValue().bizType());
        assertTrue(cmd.getValue().afterPayload().contains("assigneeId"), "after 应含 assigneeId");
        assertTrue(cmd.getValue().afterPayload().contains("dueDate"));
    }

    @Test
    void rejectsWhenNoEffectiveChange() {
        when(taskMapper.selectById(1L)).thenReturn(task());
        assertThrows(BizException.class, () -> service.submit(1L, new TaskChangeRequestDTO(
                "task_baseline", "无", null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 100L)));
        verify(changeService, never()).submit(any());
    }

    @Test
    void rejectsWhenDueBeforeStart() {
        when(taskMapper.selectById(1L)).thenReturn(task());
        assertThrows(BizException.class, () -> service.submit(1L, new TaskChangeRequestDTO(
                "task_baseline", "逆序", null, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 5), null)));
        verify(changeService, never()).submit(any());
    }

    @Test
    void rejectsIllegalChangeType() {
        assertThrows(BizException.class, () -> service.submit(1L, new TaskChangeRequestDTO(
                "bogus", "x", null, null, LocalDate.of(2026, 6, 20), null)));
        verify(changeService, never()).submit(any());
    }
}
