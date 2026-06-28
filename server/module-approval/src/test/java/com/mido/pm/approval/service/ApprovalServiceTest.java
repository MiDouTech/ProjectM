package com.mido.pm.approval.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.RelatedApprovalVO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.dto.TransferDTO;
import com.mido.pm.approval.dto.WithdrawDTO;
import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.entity.ApprovalInstance;
import com.mido.pm.approval.domain.ApproverDirectory;
import com.mido.pm.approval.domain.ApproverResolver;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批编排单测（mock mapper/事件/guard，无 DB）：提交建实例+待办+事件；通过推进结案；驳回；已结束拒绝。
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    private static final String SINGLE_NODE =
            "{\"nodes\":[{\"key\":\"n1\",\"name\":\"审批\",\"approvers\":[100],\"mode\":\"or\"}]}";
    private static final String TWO_NODE =
            "{\"nodes\":["
            + "{\"key\":\"n1\",\"name\":\"一审\",\"approvers\":[100],\"mode\":\"or\"},"
            + "{\"key\":\"n2\",\"name\":\"二审\",\"approvers\":[200,300],\"mode\":\"or\"}]}";
    /** 首节点审批人为空（应自动跳过并落到 n2） */
    private static final String EMPTY_FIRST_TWO_NODE =
            "{\"nodes\":["
            + "{\"key\":\"n1\",\"name\":\"一审\",\"approvers\":[],\"mode\":\"or\"},"
            + "{\"key\":\"n2\",\"name\":\"二审\",\"approvers\":[200],\"mode\":\"or\"}]}";
    /** 唯一节点审批人为空（应自动通过，不留死锁实例） */
    private static final String SINGLE_EMPTY =
            "{\"nodes\":[{\"key\":\"n1\",\"name\":\"审\",\"approvers\":[],\"mode\":\"or\"}]}";

    @Mock private ApprovalFlowMapper flowMapper;
    @Mock private ApprovalInstanceMapper instanceMapper;
    @Mock private ApprovalTaskMapper taskMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private NodeGuardRegistry guardRegistry;

    private ApprovalService service;

    @BeforeEach
    void setUp() {
        // USER 型审批人解析器（directory 仅角色/部门主管用，本测试用例均为指定成员，返回空即可）
        ApproverResolver resolver = new ApproverResolver(new ApproverDirectory() {
            @Override
            public List<Long> usersByRole(Long roleId) {
                return List.of();
            }

            @Override
            public Long deptLeaderOf(Long applicantId, int levelsUp) {
                return null;
            }
        });
        service = new ApprovalService(flowMapper, instanceMapper, taskMapper,
                eventPublisher, guardRegistry, resolver, new ObjectMapper());
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
        // 事件携带首节点审批人，供通知监听器多通道通知
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("approval.submitted"), captor.capture());
        assertEquals(List.of(100L), captor.getValue().get("approverIds"));
    }

    @Test
    void submitSkipsEmptyApproverNodeAndLandsOnNext() {
        login(7);
        ApprovalFlow f = flow();
        f.setDefinition(EMPTY_FIRST_TWO_NODE);
        when(flowMapper.selectById(10L)).thenReturn(f);

        service.submit(new SubmitDTO(10L, "project_init", 1L, Map.of("category", "O")));

        // n1 审批人空 → 发跳过告警；落到 n2，为 200 建待办
        verify(eventPublisher).publish(eq("approval.node.skipped"), any());
        ArgumentCaptor<ApprovalTask> taskCap = ArgumentCaptor.forClass(ApprovalTask.class);
        verify(taskMapper).insert(taskCap.capture());
        assertEquals(200L, taskCap.getValue().getApproverId());
        assertEquals("n2", taskCap.getValue().getNode());
    }

    @Test
    void submitAllEmptyNodesAutoApprovesWithoutDeadlock() {
        login(7);
        ApprovalFlow f = flow();
        f.setDefinition(SINGLE_EMPTY);
        when(flowMapper.selectById(10L)).thenReturn(f);

        service.submit(new SubmitDTO(10L, "project_init", 1L, Map.of()));

        verify(eventPublisher).publish(eq("approval.node.skipped"), any());
        verify(eventPublisher).publish(eq("approval.approved"), any());
        verify(taskMapper, never()).insert(any(ApprovalTask.class));
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
        approved.setAction(ApprovalTask.ACTION_APPROVE);
        when(taskMapper.selectList(any())).thenReturn(List.of(approved));

        service.act(1L, new ActDTO("approve", "ok"));

        assertEquals(ApprovalInstance.STATUS_APPROVED, inst.getStatus());
        verify(eventPublisher).publish(eq("approval.node.approved"), any());
        verify(eventPublisher).publish(eq("approval.approved"), any());
    }

    @Test
    void approveFirstNodeCarriesNextApproversAndAdvances() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        ApprovalFlow twoNode = flow();
        twoNode.setDefinition(TWO_NODE);
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(twoNode);
        ApprovalTask todo = new ApprovalTask();
        todo.setInstanceId(1L);
        todo.setNode("n1");
        todo.setApproverId(100L);
        when(taskMapper.selectOne(any())).thenReturn(todo);
        ApprovalTask approved = new ApprovalTask();
        approved.setApproverId(100L);
        approved.setAction(ApprovalTask.ACTION_APPROVE);
        when(taskMapper.selectList(any())).thenReturn(List.of(approved));

        service.act(1L, new ActDTO("approve", "ok"));

        // 推进到 n2、为其建待办、事件携带下一节点审批人
        assertEquals("n2", inst.getCurrentNode());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("approval.node.approved"), captor.capture());
        assertEquals(List.of(200L, 300L), captor.getValue().get("nextApproverIds"));
        assertEquals("n2", captor.getValue().get("nextNode"));
        verify(eventPublisher, never()).publish(eq("approval.approved"), any());
    }

    @Test
    void rejectMarksInstanceRejected() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        inst.setBizType("project_init");
        inst.setBizId(99L);
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask todo = new ApprovalTask();
        todo.setApproverId(100L);
        todo.setNode("n1");
        when(taskMapper.selectOne(any())).thenReturn(todo);

        service.act(1L, new ActDTO("reject", "驳回理由"));

        assertEquals(ApprovalInstance.STATUS_REJECTED, inst.getStatus());
        // 驳回事件须携带 bizType/bizId，业务侧才能据此回退状态
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("approval.rejected"), captor.capture());
        assertEquals(99L, captor.getValue().get("bizId"));
        assertEquals("project_init", captor.getValue().get("bizType"));
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

    @Test
    void myInitiatedReturnsMySubmissionsAcrossBizTypes() {
        login(7);
        ApprovalInstance proj = instanceWithStatus(10L, ApprovalInstance.STATUS_PENDING);
        proj.setFormData("{\"projectName\":\"立项A\"}");
        proj.setCreateTime(LocalDateTime.now());
        ApprovalInstance change = instanceWithStatus(20L, ApprovalInstance.STATUS_APPROVED);
        change.setBizType("change");
        when(instanceMapper.selectList(any())).thenReturn(List.of(change, proj));

        var result = service.myInitiated();

        assertEquals(2, result.size());
        // 跨 bizType 一并返回，状态原样透出（供前端按类型筛选/着色）
        assertEquals("change", result.get(0).bizType());
        assertEquals(ApprovalInstance.STATUS_APPROVED, result.get(0).status());
        assertEquals(ApprovalInstance.STATUS_PENDING, result.get(1).status());
        // title 取自 formData、submittedAt 取自 createTime，正向覆盖透出字段
        assertEquals("立项A", result.get(1).title());
        assertNotNull(result.get(1).submittedAt());
        // 必须按发起人过滤（防越权返回他人审批）：查询片段须含 applicant 列条件。
        // 单测无 Spring，先初始化 lambda 列缓存，wrapper 方能解析列名供断言。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalInstance.class);
        ArgumentCaptor<AbstractWrapper> cap = ArgumentCaptor.forClass(AbstractWrapper.class);
        verify(instanceMapper).selectList(cap.capture());
        String sql = cap.getValue().getTargetSql().toLowerCase();
        assertTrue(sql.contains("applicant"), "myInitiated 必须按当前用户 applicantId 过滤");
    }

    @Test
    void myInitiatedReturnsEmptyWhenNotLoggedIn() {
        // 未登录（无 UserContext）直接返回空，不触达 mapper
        var result = service.myInitiated();

        assertEquals(0, result.size());
        verify(instanceMapper, never()).selectList(any());
    }

    @Test
    void myRelatedMergesInitiatedToActAndProcessedWithRoleFlags() {
        login(100);
        // 我名下任务：实例10未办(待我处理)、实例20已办(我已处理)、实例40未办(同时也是我发起→去重)
        when(taskMapper.selectList(any())).thenReturn(List.of(
                relatedTask(10L, null), relatedTask(20L, ApprovalTask.ACTION_APPROVE), relatedTask(40L, null)));
        // 我发起的实例：30、40
        when(instanceMapper.selectList(any())).thenReturn(List.of(
                relatedInst(30L, ApprovalInstance.STATUS_PENDING, 100L),
                relatedInst(40L, ApprovalInstance.STATUS_PENDING, 100L)));
        // 合并去重后批量回填全部实例（10/20 申请人为他人，30/40 为本人）
        when(instanceMapper.selectBatchIds(any())).thenReturn(List.of(
                relatedInst(10L, ApprovalInstance.STATUS_PENDING, 7L),
                relatedInst(20L, ApprovalInstance.STATUS_APPROVED, 7L),
                relatedInst(30L, ApprovalInstance.STATUS_PENDING, 100L),
                relatedInst(40L, ApprovalInstance.STATUS_PENDING, 100L)));

        var result = service.myRelated();

        // 四实例各一条且按 id 倒序（新→旧）
        assertEquals(4, result.size());
        assertEquals(40L, result.get(0).instanceId());
        assertEquals(10L, result.get(3).instanceId());
        var byId = result.stream().collect(Collectors.toMap(RelatedApprovalVO::instanceId, v -> v));
        // 实例10：待我处理
        assertTrue(byId.get(10L).mineToAct());
        assertFalse(byId.get(10L).iInitiated());
        assertFalse(byId.get(10L).processedByMe());
        // 实例20：我已处理（已结案，非待我处理）
        assertFalse(byId.get(20L).mineToAct());
        assertTrue(byId.get(20L).processedByMe());
        // 实例30：仅我发起
        assertTrue(byId.get(30L).iInitiated());
        assertFalse(byId.get(30L).mineToAct());
        // 实例40：我发起 + 待我处理（既在 task 又在 initiated，去重为一条且双标记）
        assertTrue(byId.get(40L).iInitiated());
        assertTrue(byId.get(40L).mineToAct());
    }

    @Test
    void myRelatedReturnsEmptyWhenNotLoggedIn() {
        var result = service.myRelated();

        assertEquals(0, result.size());
        verify(taskMapper, never()).selectList(any());
        verify(instanceMapper, never()).selectList(any());
    }

    @Test
    void withdrawByApplicantMarksWithdrawnAndCarriesApprovers() {
        login(7);
        ApprovalInstance inst = pendingInstance();
        inst.setApplicantId(7L);
        inst.setBizType("project_init");
        inst.setBizId(1L);
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        // 当前节点待办审批人 → 撤回事件须携带，供通知监听器通知其无需处理
        ApprovalTask pending = new ApprovalTask();
        pending.setApproverId(55L);
        pending.setNode("n1");
        when(taskMapper.selectList(any())).thenReturn(List.of(pending));

        service.withdraw(1L, new WithdrawDTO("不做了"));

        assertEquals(ApprovalInstance.STATUS_WITHDRAWN, inst.getStatus());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("approval.withdrawn"), captor.capture());
        assertEquals(List.of(55L), captor.getValue().get("approverIds"));
    }

    @Test
    void withdrawByNonApplicantForbidden() {
        login(8);
        ApprovalInstance inst = pendingInstance();
        inst.setApplicantId(7L);
        when(instanceMapper.selectById(1L)).thenReturn(inst);

        assertThrows(BizException.class, () -> service.withdraw(1L, new WithdrawDTO(null)));
        verify(eventPublisher, never()).publish(eq("approval.withdrawn"), any());
    }

    @Test
    void withdrawNonPendingConflict() {
        login(7);
        ApprovalInstance inst = pendingInstance();
        inst.setStatus(ApprovalInstance.STATUS_APPROVED);
        inst.setApplicantId(7L);
        when(instanceMapper.selectById(1L)).thenReturn(inst);

        assertThrows(BizException.class, () -> service.withdraw(1L, new WithdrawDTO(null)));
    }

    @Test
    void transferCreatesHandoffTaskAndEvent() {
        login(100);
        ApprovalInstance inst = pendingInstance();
        inst.setBizType("project_init");
        inst.setBizId(7L);
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask todo = new ApprovalTask();
        todo.setInstanceId(1L);
        todo.setNode("n1");
        todo.setApproverId(100L);
        when(taskMapper.selectOne(any())).thenReturn(todo);

        service.transfer(1L, new TransferDTO(200L, "出差代审"));

        assertEquals(ApprovalTask.ACTION_TRANSFER, todo.getAction());
        verify(taskMapper).insert(any(ApprovalTask.class)); // 为受让人建新待办
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("approval.transferred"), captor.capture());
        assertEquals(200L, captor.getValue().get("toUserId"));
        assertEquals(100L, captor.getValue().get("fromUserId"));
    }

    @Test
    void transferToSelfRejected() {
        login(100);
        when(instanceMapper.selectById(1L)).thenReturn(pendingInstance());

        assertThrows(BizException.class, () -> service.transfer(1L, new TransferDTO(100L, null)));
        verify(eventPublisher, never()).publish(eq("approval.transferred"), any());
    }

    @Test
    void transferToExistingApproverRejected() {
        login(100);
        when(instanceMapper.selectById(1L)).thenReturn(pendingInstance());
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask mine = new ApprovalTask();
        mine.setNode("n1");
        mine.setApproverId(100L);
        when(taskMapper.selectOne(any())).thenReturn(mine);
        when(taskMapper.selectCount(any())).thenReturn(1L); // 受让人在该节点已有待办

        assertThrows(BizException.class, () -> service.transfer(1L, new TransferDTO(200L, null)));
        verify(eventPublisher, never()).publish(eq("approval.transferred"), any());
    }

    @Test
    void transferWithoutTodoForbidden() {
        login(100);
        when(instanceMapper.selectById(1L)).thenReturn(pendingInstance());
        when(flowMapper.selectById(10L)).thenReturn(flow());
        when(taskMapper.selectOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.transfer(1L, new TransferDTO(200L, null)));
    }

    @Test
    void getInstanceExposesCurrentNodeAndPendingApprovers() {
        ApprovalInstance inst = pendingInstance();
        when(instanceMapper.selectById(1L)).thenReturn(inst);
        when(flowMapper.selectById(10L)).thenReturn(flow());
        ApprovalTask pending = new ApprovalTask();
        pending.setApproverId(100L);
        pending.setNode("n1");
        when(taskMapper.selectList(any())).thenReturn(List.of(pending));

        var vo = service.getInstance(1L);

        assertEquals("审批", vo.currentNodeName());
        assertEquals("or", vo.currentMode());
        assertEquals(List.of(100L), vo.pendingApproverIds());
        assertEquals(List.of(), vo.approvedApproverIds());
    }

    private ApprovalTask pendingTask(long instanceId) {
        ApprovalTask t = new ApprovalTask();
        t.setInstanceId(instanceId);
        t.setNode("n1");
        t.setApproverId(100L);
        return t;
    }

    private ApprovalTask relatedTask(long instanceId, String action) {
        ApprovalTask t = pendingTask(instanceId);
        t.setAction(action);
        return t;
    }

    private ApprovalInstance relatedInst(long id, String status, long applicantId) {
        ApprovalInstance i = instanceWithStatus(id, status);
        i.setApplicantId(applicantId);
        return i;
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
