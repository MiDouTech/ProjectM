package com.mido.pm.project.service;

import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.goal.service.GoalService;
import com.mido.pm.project.dto.InitiationFormDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 立项审批提交单测（mock 审批引擎/项目服务/Identity/类型解析，无 DB）。
 * 审批流由项目类型 default_flow_id 决定，职级门槛由类型 min_job_level 决定。
 */
@ExtendWith(MockitoExtension.class)
class ProjectInitServiceTest {

    @Mock private PmProjectMapper projectMapper;
    @Mock private ProjectService projectService;
    @Mock private ApprovalService approvalService;
    @Mock private IdentityProvider identityProvider;
    @Mock private ProjectTypeResolver projectTypeResolver;
    @Mock private GoalService goalService;

    private ProjectInitService service;

    @BeforeEach
    void setUp() {
        service = new ProjectInitService(projectMapper, projectService,
                approvalService, identityProvider, projectTypeResolver, goalService);
    }

    private PmProject draft(String category, Long leaderId) {
        PmProject p = new PmProject();
        p.setId(1L);
        p.setStatus("草稿");
        p.setCategory(category);
        p.setLeaderId(leaderId);
        return p;
    }

    private PmProjectType type(Long flowId, String minJobLevel) {
        PmProjectType t = new PmProjectType();
        t.setName("战略级");
        t.setDefaultFlowId(flowId);
        t.setMinJobLevel(minJobLevel);
        return t;
    }

    @Test
    void submitResolvesFlowSubmitsAndTransitionsToApproving() {
        PmProject p = draft("S", 1L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        when(projectTypeResolver.require("S", null)).thenReturn(type(99L, "L3"));
        UserPrincipal leader = new UserPrincipal();
        leader.setJobLevel("L3");
        when(identityProvider.loadById(1L)).thenReturn(Optional.of(leader));
        when(approvalService.submit(any(SubmitDTO.class))).thenReturn(500L);

        Long instanceId = service.submitApproval(1L,
                new InitiationFormDTO("项目目标", null, null, "干系人初稿", "价值假设"));

        assertEquals(500L, instanceId);
        // 提交到类型绑定的 flowId=99
        verify(approvalService).submit(argThat((SubmitDTO dto) -> dto.flowId() == 99L));
        verify(projectService).transition(eq(1L),
                argThat((ProjectTransitionDTO dto) -> "审批中".equals(dto.targetStatus())));
    }

    @Test
    void rejectsWhenTypeHasNoBoundFlow() {
        PmProject p = draft("S", 1L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        when(projectTypeResolver.require("S", null)).thenReturn(type(null, "L3"));
        assertThrows(BizException.class, () ->
                service.submitApproval(1L, new InitiationFormDTO("目标", null, null, null, null)));
    }

    @Test
    void rejectsWhenTypeRequiresGoalAlignmentButNoneAligned() {
        PmProject p = draft("S", 1L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        PmProjectType t = type(99L, "L3");
        t.setRequireGoalAlignment(1);
        when(projectTypeResolver.require("S", null)).thenReturn(t);
        when(goalService.listGoalsByTarget("project", 1L)).thenReturn(java.util.List.of());
        assertThrows(BizException.class, () ->
                service.submitApproval(1L, new InitiationFormDTO("目标", null, null, null, null)));
    }

    @Test
    void nonDraftProjectRejected() {
        PmProject p = draft("S", 1L);
        p.setStatus("进行中");
        when(projectMapper.selectById(1L)).thenReturn(p);
        assertThrows(BizException.class, () ->
                service.submitApproval(1L, new InitiationFormDTO(null, null, null, null, null)));
    }
}
