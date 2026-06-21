package com.mido.pm.approval.outcome;

import com.mido.pm.common.outbox.DomainEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批结果路由单测：approved/rejected/withdrawn 按 bizType 路由到 handler；
 * 非审批结果事件、未注册 bizType、payload 非 Map、bizId 非数字均忽略；handler 异常被兜底吞掉。
 */
class ApprovalOutcomeRouterTest {

    private ApprovalOutcomeHandler handler;
    private ApprovalOutcomeRouter router;

    @BeforeEach
    void setUp() {
        handler = mock(ApprovalOutcomeHandler.class);
        when(handler.bizType()).thenReturn("x");
        router = new ApprovalOutcomeRouter(new ApprovalBizTypeRegistry(List.of(handler)));
    }

    private DomainEventMessage msg(String type, Object bizType, Object bizId) {
        return new DomainEventMessage(type, Map.of("bizType", bizType, "bizId", bizId), 1L);
    }

    @Test
    void approvedRoutesToHandler() {
        router.onDomainEvent(msg("approval.approved", "x", 5L));
        verify(handler).onApproved(5L);
    }

    @Test
    void rejectedRoutesToHandler() {
        router.onDomainEvent(msg("approval.rejected", "x", 5L));
        verify(handler).onRejected(5L);
    }

    @Test
    void withdrawnRoutesToHandler() {
        router.onDomainEvent(msg("approval.withdrawn", "x", 5L));
        verify(handler).onWithdrawn(5L);
    }

    @Test
    void unknownBizTypeIgnored() {
        router.onDomainEvent(msg("approval.approved", "other", 5L));
        verify(handler, never()).onApproved(anyLong());
    }

    @Test
    void nonOutcomeEventIgnored() {
        router.onDomainEvent(msg("approval.submitted", "x", 5L));
        verify(handler, never()).onApproved(anyLong());
    }

    @Test
    void nonMapPayloadIgnored() {
        router.onDomainEvent(new DomainEventMessage("approval.approved", "not-a-map", 1L));
        verify(handler, never()).onApproved(anyLong());
    }

    @Test
    void nonNumberBizIdIgnored() {
        router.onDomainEvent(msg("approval.approved", "x", "abc"));
        verify(handler, never()).onApproved(anyLong());
    }

    @Test
    void handlerExceptionSwallowed() {
        doThrow(new RuntimeException("boom")).when(handler).onApproved(anyLong());
        assertDoesNotThrow(() -> router.onDomainEvent(msg("approval.approved", "x", 5L)));
    }
}
