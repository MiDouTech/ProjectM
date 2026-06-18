package com.mido.pm.project.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 审批通过监听器单测：approval.approved + project_init → 驱动项目注册；其余事件忽略。
 */
@ExtendWith(MockitoExtension.class)
class ProjectApprovalListenerTest {

    @Mock private ProjectService projectService;
    @InjectMocks private ProjectApprovalListener listener;

    @Test
    void approvedProjectInitTriggersRegister() {
        listener.onDomainEvent(new DomainEventMessage("approval.approved",
                Map.of("bizType", "project_init", "bizId", 42L), 1L));

        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "已注册".equals(d.targetStatus()) && Boolean.TRUE.equals(d.approvalPassed())));
    }

    @Test
    void withdrawnProjectInitTriggersDraft() {
        listener.onDomainEvent(new DomainEventMessage("approval.withdrawn",
                Map.of("bizType", "project_init", "bizId", 42L), 1L));

        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "草稿".equals(d.targetStatus())));
    }

    @Test
    void rejectedProjectInitTriggersDraft() {
        listener.onDomainEvent(new DomainEventMessage("approval.rejected",
                Map.of("bizType", "project_init", "bizId", 42L), 1L));

        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "草稿".equals(d.targetStatus())));
    }

    @Test
    void otherEventIgnored() {
        listener.onDomainEvent(new DomainEventMessage("approval.submitted",
                Map.of("bizType", "project_init", "bizId", 42L), 1L));
        verifyNoInteractions(projectService);
    }

    @Test
    void otherBizTypeIgnored() {
        listener.onDomainEvent(new DomainEventMessage("approval.approved",
                Map.of("bizType", "cost", "bizId", 42L), 1L));
        verifyNoInteractions(projectService);
    }
}
