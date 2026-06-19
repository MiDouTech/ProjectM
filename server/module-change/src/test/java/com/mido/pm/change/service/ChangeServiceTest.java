package com.mido.pm.change.service;

import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.change.domain.ChangeStatus;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.entity.PmChangePolicy;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.change.mapper.PmChangePolicyMapper;
import com.mido.pm.change.mapper.PmChangeRequestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 变更提交分流：必审→审批引擎；免审→即时应用；未绑流/有进行中变更→拒；审批回流终结。 */
@ExtendWith(MockitoExtension.class)
class ChangeServiceTest {

    @Mock private PmChangeRequestMapper changeMapper;
    @Mock private PmChangePolicyMapper policyMapper;
    @Mock private ApprovalService approvalService;
    @Mock private ChangeApplyService applyService;
    @Mock private DomainEventPublisher publisher;
    @InjectMocks private ChangeService service;

    private ChangeSubmitCmd cmd(String changeType) {
        return new ChangeSubmitCmd("goal", 7L, changeType, "标题", "事由", null,
                "{}", "{\"metricTarget\":200}", Map.of("changeType", changeType));
    }

    private PmChangePolicy policy(int requireApproval, Long flowId) {
        PmChangePolicy p = new PmChangePolicy();
        p.setRequireApproval(requireApproval);
        p.setFlowId(flowId);
        p.setEnabled(1);
        return p;
    }

    @Test
    void mandatoryChangeGoesToApproval() {
        when(changeMapper.selectCount(any())).thenReturn(0L);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy(1, 77L)));
        when(approvalService.submit(any(SubmitDTO.class))).thenReturn(900L);

        service.submit(cmd("goal_target"));

        verify(approvalService).submit(any(SubmitDTO.class));
        verify(applyService, never()).apply(anyLong());
    }

    @Test
    void exemptChangeAppliesImmediately() {
        when(changeMapper.selectCount(any())).thenReturn(0L);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy(0, null)));

        service.submit(cmd("goal_owner"));

        verify(applyService).apply(any());
        verify(approvalService, never()).submit(any(SubmitDTO.class));
    }

    @Test
    void mandatoryWithoutFlowRejected() {
        when(changeMapper.selectCount(any())).thenReturn(0L);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy(1, null)));
        assertThrows(BizException.class, () -> service.submit(cmd("goal_target")));
    }

    @Test
    void blockedWhenPendingChangeExists() {
        when(changeMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () -> service.submit(cmd("goal_target")));
        verify(approvalService, never()).submit(any(SubmitDTO.class));
    }

    @Test
    void concurrentInsertRaceMappedToConflict() {
        // hasPending 预检通过(0)，但并发下 DB 唯一索引 uk_pending 触发 DuplicateKeyException → 转业务冲突
        when(changeMapper.selectCount(any())).thenReturn(0L);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DuplicateKeyException("uk_pending"))
                .when(changeMapper).insert(any(PmChangeRequest.class));
        assertThrows(BizException.class, () -> service.submit(cmd("goal_target")));
        verify(approvalService, never()).submit(any(SubmitDTO.class));
    }

    @Test
    void approvalClosedRejectsChange() {
        PmChangeRequest cr = new PmChangeRequest();
        cr.setId(5L);
        cr.setStatus(ChangeStatus.PENDING);
        when(changeMapper.selectById(5L)).thenReturn(cr);

        service.onApprovalClosed(5L, true);

        assertEquals(ChangeStatus.REJECTED, cr.getStatus());
        verify(changeMapper).updateById(cr);
    }
}
