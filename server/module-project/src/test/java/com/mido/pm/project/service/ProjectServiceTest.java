package com.mido.pm.project.service;

import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.dto.ProjectCreateDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.dto.ProjectUpdateDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectMemberMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 项目流转编排单测（mock mapper/事件/Identity，无 DB）：
 * 非法流转拒绝且不落库不发事件；合法流转发事件；注册职级/审批结果 guard 生效。
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private PmProjectMapper projectMapper;
    @Mock
    private PmProjectMemberMapper memberMapper;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private IdentityProvider identityProvider;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ProjectTypeResolver projectTypeResolver;
    @InjectMocks
    private ProjectService service;

    private PmProject project(String status, String category, Long leaderId) {
        PmProject p = new PmProject();
        p.setId(1L);
        p.setStatus(status);
        p.setCategory(category);
        p.setLeaderId(leaderId);
        return p;
    }

    /** 构造一个项目类型桩：minJobLevel 门槛 + requiresNpss。 */
    private com.mido.pm.project.entity.PmProjectType type(String minJobLevel, int requiresNpss) {
        com.mido.pm.project.entity.PmProjectType t = new com.mido.pm.project.entity.PmProjectType();
        t.setMinJobLevel(minJobLevel);
        t.setRequiresNpss(requiresNpss);
        return t;
    }

    private UserPrincipal leaderWithLevel(String level) {
        UserPrincipal up = new UserPrincipal();
        up.setJobLevel(level);
        return up;
    }

    @Test
    void illegalTransitionRejectedNoSideEffects() {
        when(projectMapper.selectById(1L)).thenReturn(project("草稿", "S", 1L));
        assertThrows(BizException.class,
                () -> service.transition(1L, new ProjectTransitionDTO("进行中", null)));
        verify(projectMapper, never()).updateById(any(PmProject.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void legalTransitionPersistsAndPublishes() {
        when(projectMapper.selectById(1L)).thenReturn(project("草稿", "S", 1L));
        service.transition(1L, new ProjectTransitionDTO("审批中", null));
        verify(projectMapper).updateById(any(PmProject.class));
        verify(eventPublisher).publish(eq("project.status.changed"), any());
    }

    @Test
    void registerRejectedWhenApprovalNotPassed() {
        when(projectMapper.selectById(1L)).thenReturn(project("审批中", "S", 9L));
        assertThrows(BizException.class,
                () -> service.transition(1L, new ProjectTransitionDTO("已注册", false)));
        verify(projectMapper, never()).updateById(any(PmProject.class));
    }

    @Test
    void registerRejectedWhenApprovalFlagAbsent() {
        // 严肃约束：未显式审批通过(null)也不得注册
        when(projectMapper.selectById(1L)).thenReturn(project("审批中", "S", 9L));
        assertThrows(BizException.class,
                () -> service.transition(1L, new ProjectTransitionDTO("已注册", null)));
        verify(projectMapper, never()).updateById(any(PmProject.class));
    }

    @Test
    void registerRejectedWhenLeaderJobLevelTooLow() {
        when(projectMapper.selectById(1L)).thenReturn(project("审批中", "S", 9L));
        when(projectTypeResolver.require("S", null)).thenReturn(type("L3", 1));
        when(identityProvider.loadById(9L)).thenReturn(Optional.of(leaderWithLevel("L2")));
        assertThrows(BizException.class,
                () -> service.transition(1L, new ProjectTransitionDTO("已注册", true)));
        verify(projectMapper, never()).updateById(any(PmProject.class));
    }

    @Test
    void manualTransitionToRegisteredRejected() {
        // 严肃约束：公开手动流转不得设为 已注册（即便伪造 approvalPassed=true）
        assertThrows(BizException.class,
                () -> service.transitionManual(1L, new ProjectTransitionDTO("已注册", true)));
        verifyNoInteractions(projectMapper, eventPublisher);
    }

    @Test
    void createRecordsActivity() {
        when(projectTypeResolver.require("O", null)).thenReturn(type("L2", 1));
        service.create(new ProjectCreateDTO("项目A", "O", null, 1L, null, null, null, null, null, null));
        verify(auditLogService).record(eq("project"), any(), eq(AuditActions.CREATED), any());
    }

    @Test
    void createSetsDeptFromLeader() {
        // 归属部门 = leader 所属部门（数据范围用）
        com.mido.pm.provider.identity.UserPrincipal leader = new com.mido.pm.provider.identity.UserPrincipal();
        leader.setDeptId(88L);
        when(identityProvider.loadById(1L)).thenReturn(java.util.Optional.of(leader));
        when(projectTypeResolver.require("O", null)).thenReturn(type("L2", 1));

        ArgumentCaptor<PmProject> captor = ArgumentCaptor.forClass(PmProject.class);
        service.create(new ProjectCreateDTO("项目A", "O", null, 1L, null, null, null, null, null, null));

        verify(projectMapper).insert(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(88L, captor.getValue().getDeptId());
    }

    @Test
    void transitionRecordsStatusChanged() {
        when(projectMapper.selectById(1L)).thenReturn(project("草稿", "S", 1L));
        service.transition(1L, new ProjectTransitionDTO("审批中", null));
        verify(auditLogService).record(eq("project"), eq(1L), eq(AuditActions.STATUS_CHANGED), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateRecordsLeaderChangeOnly() {
        PmProject p = project("草稿", "O", 1L);
        p.setName("旧名");
        when(projectMapper.selectById(1L)).thenReturn(p);

        // 仅改负责人（1→2），名称不变
        service.update(1L, new ProjectUpdateDTO("旧名", null, 2L, null, null, null, null));

        ArgumentCaptor<Object> detail = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).record(eq("project"), eq(1L), eq(AuditActions.UPDATED), detail.capture());
        List<Map<String, Object>> changes =
                (List<Map<String, Object>>) ((Map<String, Object>) detail.getValue()).get("changes");
        assertEquals(1, changes.size());
        assertEquals("leaderId", changes.get(0).get("field"));
        assertEquals(1L, changes.get(0).get("from"));
        assertEquals(2L, changes.get(0).get("to"));
    }

    @Test
    void closeNpssProjectSchedulesValueReview() {
        PmProject p = project("结果验收", "S", 1L);
        p.setRequiresNpss(1);
        when(projectMapper.selectById(1L)).thenReturn(p);

        service.transition(1L, new ProjectTransitionDTO("已结案", null));

        assertEquals("已结案", p.getStatus());
        assertNotNull(p.getValueReviewDueDate(), "NPSS 项目结案应安排价值验收日");
    }

    @Test
    void closeNonNpssProjectSkipsValueReview() {
        PmProject p = project("结果验收", "O", 1L);
        p.setSubCategory("定向整改");
        p.setRequiresNpss(0);
        when(projectMapper.selectById(1L)).thenReturn(p);

        service.transition(1L, new ProjectTransitionDTO("已结案", null));

        assertEquals("已结案", p.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(p.getValueReviewDueDate(),
                "非 NPSS 项目结案不应安排价值验收日");
    }

    @Test
    void createOperationRectifyDefaultsToNonNpss() {
        when(projectTypeResolver.require("O", "定向整改")).thenReturn(type("L2", 0));
        ArgumentCaptor<PmProject> captor = ArgumentCaptor.forClass(PmProject.class);
        service.create(new ProjectCreateDTO("整改A", "O", "定向整改", 1L, null, null, null, null, null, null));
        verify(projectMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getRequiresNpss(), "定向整改默认不走 NPSS");
    }

    @Test
    void registerSucceedsWithQualifiedLeaderAndEmitsRegistered() {
        PmProject p = project("审批中", "S", 9L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        when(projectTypeResolver.require("S", null)).thenReturn(type("L3", 1));
        when(identityProvider.loadById(9L)).thenReturn(Optional.of(leaderWithLevel("L3")));

        service.transition(1L, new ProjectTransitionDTO("已注册", true));

        assertEquals("已注册", p.getStatus());
        assertNotNull(p.getPmoRegisteredAt(), "注册应写 pmo_registered_at");
        verify(eventPublisher).publish(eq("project.status.changed"), any());
        verify(eventPublisher).publish(eq("project.registered"), any());
    }
}
