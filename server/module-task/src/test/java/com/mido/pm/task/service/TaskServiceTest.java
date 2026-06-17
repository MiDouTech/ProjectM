package com.mido.pm.task.service;

import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.task.dto.KanbanColumnVO;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.dto.TaskUpdateDTO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.common.audit.AuditActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务服务编排单测（mock mapper/事件，无 DB）：建任务发事件、指派、状态流转合法/非法、看板分组。
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private PmTaskMapper taskMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private TaskService service;

    private PmTask task(String status) {
        PmTask t = new PmTask();
        t.setId(1L);
        t.setProjectId(9L);
        t.setStatus(status);
        return t;
    }

    @Test
    void createEmitsCreatedOnly() {
        service.create(new TaskCreateDTO("写文档", 9L, null, null, 1, null, null, null, 0, null));
        verify(taskMapper).insert(any(PmTask.class));
        verify(eventPublisher).publish(eq("task.created"), any());
        verify(eventPublisher, never()).publish(eq("task.assigned"), any());
    }

    @Test
    void createWithAssigneeEmitsAssigned() {
        service.create(new TaskCreateDTO("写文档", 9L, null, 100L, 1, null, null, null, 0, null));
        verify(eventPublisher).publish(eq("task.created"), any());
        verify(eventPublisher).publish(eq("task.assigned"), any());
    }

    @Test
    void changeStatusLegalPublishes() {
        when(taskMapper.selectById(1L)).thenReturn(task("未开始"));
        service.changeStatus(1L, "进行中");
        verify(taskMapper).updateById(any(PmTask.class));
        verify(eventPublisher).publish(eq("task.status.changed"), any());
    }

    @Test
    void changeStatusIllegalRejected() {
        when(taskMapper.selectById(1L)).thenReturn(task("未开始"));
        assertThrows(BizException.class, () -> service.changeStatus(1L, "已验收"));
        verify(taskMapper, never()).updateById(any(PmTask.class));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void assignEmitsAssigned() {
        when(taskMapper.selectById(1L)).thenReturn(task("未开始"));
        service.assign(1L, 100L);
        verify(eventPublisher).publish(eq("task.assigned"), any());
    }

    @Test
    void changeStatusRecordsActivity() {
        when(taskMapper.selectById(1L)).thenReturn(task("未开始"));
        service.changeStatus(1L, "进行中");
        verify(auditLogService).record(eq("task"), eq(1L), eq(AuditActions.STATUS_CHANGED), any());
    }

    @Test
    void assignRecordsActivityWithFromTo() {
        when(taskMapper.selectById(1L)).thenReturn(task("未开始"));
        service.assign(1L, 100L);
        verify(auditLogService).record(eq("task"), eq(1L), eq(AuditActions.ASSIGNED), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateRecordsOnlyChangedFields() {
        PmTask t = task("未开始");
        t.setTitle("旧标题");
        t.setPriority(1);
        when(taskMapper.selectById(1L)).thenReturn(t);

        // 仅改标题，优先级不变
        service.update(1L, new TaskUpdateDTO("新标题", 1, null, null, null, null, null));

        ArgumentCaptor<Object> detail = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).record(eq("task"), eq(1L), eq(AuditActions.UPDATED), detail.capture());
        List<Map<String, Object>> changes = (List<Map<String, Object>>) ((Map<String, Object>) detail.getValue()).get("changes");
        assertEquals(1, changes.size());
        assertEquals("title", changes.get(0).get("field"));
        assertEquals("新标题", changes.get(0).get("to"));
    }

    @Test
    void updateWithoutChangeRecordsNothing() {
        PmTask t = task("未开始");
        t.setTitle("标题");
        when(taskMapper.selectById(1L)).thenReturn(t);
        service.update(1L, new TaskUpdateDTO("标题", null, null, null, null, null, null));
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void kanbanGroupsByStatusInOrder() {
        when(taskMapper.selectList(any())).thenReturn(List.of(
                task("未开始"), task("进行中"), task("进行中")));

        List<KanbanColumnVO> columns = service.kanban(9L);

        assertEquals(4, columns.size());
        assertEquals("未开始", columns.get(0).status());
        assertEquals(1, columns.get(0).tasks().size());
        assertEquals("进行中", columns.get(1).status());
        assertEquals(2, columns.get(1).tasks().size());
        assertEquals("已完成", columns.get(2).status());
        assertEquals(0, columns.get(2).tasks().size());
        assertEquals("已验收", columns.get(3).status());
    }
}
