package com.mido.pm.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.entity.ApprovalInstance;
import com.mido.pm.approval.entity.ApprovalTask;
import com.mido.pm.approval.guard.NodeGuardRegistry;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.approval.mapper.ApprovalInstanceMapper;
import com.mido.pm.approval.mapper.ApprovalTaskMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批编排单测（mock mapper/事件/guard，无 DB）：提交建实例+待办+事件；通过推进结案；驳回；已结束拒绝。
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    private static final String SINGLE_NODE =
            "{\"nodes\":[{\"key\":\"n1\",\"name\":\"审批\",\"approvers\":[100],\"mode\":\"or\"}]}";

    @Mock private ApprovalFlowMapper flowMapper;
    @Mock private ApprovalInstanceMapper instanceMapper;
    @Mock private ApprovalTaskMapper taskMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private NodeGuardRegistry guardRegistry;

    private ApprovalService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalService(flowMapper, instanceMapper, taskMapper,
                eventPublisher, guardRegistry, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void login(long uid) {
        CurrentUser u = new CurrentUser();
        u.setUserId(uid);
        UserContext.set(u);
    }

    private ApprovalFlow flow() {
        ApprovalFlow f = new ApprovalFlow();
        f.setId(10L);
        f.setBizType("project_init");
        f.setDefinition(SINGLE_NODE);
        return f;
    }

    private ApprovalInstance pendingInstance() {
        ApprovalInstance i = new ApprovalInstance();
        i.setId(1L);
        i.setFlowId(10L);
        i.setStatus(ApprovalInstance.STATUS_PENDING);
        i.setCurrentNode("n1");
        i.setFormData("{\"category\":\"O\"}");
        return i;
    }

    @Test
    void submitCreatesInstanceTaskAndEvent() {
        login(7);
        when(flowMapper.selectById(10L)).thenReturn(flow());

        service.submit(new SubmitDTO(10L, "project_init", 1L, Map.of("category", "O")));

        verify(guardRegistry).run(any(), any());
        verify(instanceMapper).insert(any(ApprovalInstance.class));
        verify(taskMapper).insert(any(ApprovalTask.class));
        verify(eventPublisher).publish(eq("approval.submitted"), any());
    }

    @Test
    void approveLastNodeApprovesInstance() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask todo = new ApprovalTask();
        todo.setInstanceId(1L);
        todo.setNode("n1");
        todo.setApproverId(100L);
        when(taskMapper.selectOne(any())).thenReturn(todo);
        ApprovalTask approved = new ApprovalTask();
        approved.setApproverId(100L);
        when(taskMapper.selectList(any())).thenReturn(List.of(approved));

        service.act(1L, new ActDTO("approve", "ok"));

        assertEquals(ApprovalInstance.STATUS_APPROVED, inst.getStatus());
        verify(eventPublisher).publish(eq("approval.node.approved"), any());
        verify(eventPublisher).publish(eq("approval.approved"), any());
    }

    @Test
    void rejectMarksInstanceRejected() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask todo = new ApprovalTask();
        todo.setApproverId(100L);
        todo.setNode("n1");
        when(taskMapper.selectOne(any())).thenReturn(todo);

        service.act(1L, new ActDTO("reject", "驳回理由"));

        assertEquals(ApprovalInstance.STATUS_REJECTED, inst.getStatus());
        verify(eventPublisher).publish(eq("approval.rejected"), any());
    }

    @Test
    void actOnFinishedInstanceRejected() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        inst.setStatus(ApprovalInstance.STATUS_APPROVED);
        when(instanceMapper.selectById(1L)).thenReturn(inst);

        assertThrows(BizException.class, () -> service.act(1L, new ActDTO("approve", null)));
    }

    @Test
    void myPendingReturnsOnlyUnprocessedOnPendingInstances() {
        login(100);
        ApprovalTask t1 = pendingTask(10L);   // 实例 pending → 计入
        ApprovalTask t2 = pendingTask(20L);   // 实例 approved → 排除
        when(taskMapper.selectList(any())).thenReturn(List.of(t1, t2));
        when(instanceMapper.selectBatchIds(any())).thenReturn(List.of(
                instanceWithStatus(10L, ApprovalInstance.STATUS_PENDING),
                instanceWithStatus(20L, ApprovalInstance.STATUS_APPROVED)));

        var result = service.myPending();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).instanceId());
        assertEquals("n1", result.get(0).node());
    }

    @Test
    void myPendingDedupesMultipleTasksOnSameInstance() {
        login(100);
        // 同一用户在同一实例的两条未处理待办（多节点）→ 去重为一条，避免前端 key 冲突
        when(taskMapper.selectList(any())).thenReturn(List.of(pendingTask(10L), pendingTask(10L)));
        when(instanceMapper.selectBatchIds(any())).thenReturn(List.of(
                instanceWithStatus(10L, ApprovalInstance.STATUS_PENDING)));

        var result = service.myPending();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).instanceId());
    }

    private ApprovalTask pendingTask(long instanceId) {
        ApprovalTask t = new ApprovalTask();
        t.setInstanceId(instanceId);
        t.setNode("n1");
        t.setApproverId(100L);
        return t;
    }

    private ApprovalInstance instanceWithStatus(long id, String status) {
        ApprovalInstance i = new ApprovalInstance();
        i.setId(id);
        i.setStatus(status);
        i.setBizType("project_init");
        i.setBizId(id);
        return i;
    }
}
