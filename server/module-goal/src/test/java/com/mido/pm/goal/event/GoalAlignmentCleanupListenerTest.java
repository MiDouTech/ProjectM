package com.mido.pm.goal.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.goal.service.GoalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 悬挂对齐清理监听单测：task.deleted / project.deleted → 按 target 清对齐链；无关事件忽略。
 */
@ExtendWith(MockitoExtension.class)
class GoalAlignmentCleanupListenerTest {

    @Mock private GoalService goalService;
    @InjectMocks private GoalAlignmentCleanupListener listener;

    @Test
    void taskDeletedCleansTaskAlignments() {
        listener.onDomainEvent(new DomainEventMessage("task.deleted", Map.of("taskId", 9L), 1L));
        verify(goalService).removeAlignmentsByTarget("task", 9L);
    }

    @Test
    void projectDeletedCleansProjectAlignments() {
        listener.onDomainEvent(new DomainEventMessage("project.deleted", Map.of("projectId", 42L), 1L));
        verify(goalService).removeAlignmentsByTarget("project", 42L);
    }

    @Test
    void unrelatedEventIgnored() {
        listener.onDomainEvent(new DomainEventMessage("project.created", Map.of("projectId", 1L), 1L));
        verify(goalService, never()).removeAlignmentsByTarget(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}
