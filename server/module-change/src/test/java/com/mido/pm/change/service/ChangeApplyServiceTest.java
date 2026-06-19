package com.mido.pm.change.service;

import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.domain.ChangeStatus;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.change.event.ChangeEvents;
import com.mido.pm.change.mapper.PmChangeRequestMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 变更应用：经匹配的 applier 回写并归档、发 change.applied；已 applied 幂等；无 applier 报错。 */
@ExtendWith(MockitoExtension.class)
class ChangeApplyServiceTest {

    @Mock private PmChangeRequestMapper changeMapper;
    @Mock private DomainEventPublisher publisher;

    private PmChangeRequest cr(String bizType, String status) {
        PmChangeRequest r = new PmChangeRequest();
        r.setId(5L);
        r.setBizType(bizType);
        r.setBizId(7L);
        r.setStatus(status);
        return r;
    }

    @Test
    void appliesViaMatchingApplierAndArchives() {
        ChangeApplier goalApplier = mock(ChangeApplier.class);
        when(goalApplier.supports("goal")).thenReturn(true);
        PmChangeRequest r = cr("goal", ChangeStatus.PENDING);
        when(changeMapper.selectById(5L)).thenReturn(r);

        new ChangeApplyService(changeMapper, List.of(goalApplier), publisher).apply(5L);

        verify(goalApplier).apply(r);
        assertEquals(ChangeStatus.APPLIED, r.getStatus());
        assertNotNull(r.getAppliedAt());
        verify(changeMapper).updateById(r);
        verify(publisher).publish(eq(ChangeEvents.APPLIED), any());
    }

    @Test
    void idempotentWhenAlreadyApplied() {
        ChangeApplier applier = mock(ChangeApplier.class);
        when(changeMapper.selectById(5L)).thenReturn(cr("goal", ChangeStatus.APPLIED));
        new ChangeApplyService(changeMapper, List.of(applier), publisher).apply(5L);
        verify(applier, never()).apply(any());
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void throwsWhenNoApplierSupportsBizType() {
        when(changeMapper.selectById(5L)).thenReturn(cr("unknown", ChangeStatus.PENDING));
        ChangeApplyService svc = new ChangeApplyService(changeMapper, List.of(), publisher);
        assertThrows(BizException.class, () -> svc.apply(5L));
    }
}
