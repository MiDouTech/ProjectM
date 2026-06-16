package com.mido.pm.project.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private DomainEventPublisher eventPublisher;
    @Mock
    private IdentityProvider identityProvider;
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
        when(identityProvider.loadById(9L)).thenReturn(Optional.of(leaderWithLevel("L2")));
        assertThrows(BizException.class,
                () -> service.transition(1L, new ProjectTransitionDTO("已注册", true)));
        verify(projectMapper, never()).updateById(any(PmProject.class));
    }

    @Test
    void registerSucceedsWithQualifiedLeaderAndEmitsRegistered() {
        PmProject p = project("审批中", "S", 9L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        when(identityProvider.loadById(9L)).thenReturn(Optional.of(leaderWithLevel("L3")));

        service.transition(1L, new ProjectTransitionDTO("已注册", true));

        assertEquals("已注册", p.getStatus());
        assertNotNull(p.getPmoRegisteredAt(), "注册应写 pmo_registered_at");
        verify(eventPublisher).publish(eq("project.status.changed"), any());
        verify(eventPublisher).publish(eq("project.registered"), any());
    }
}
