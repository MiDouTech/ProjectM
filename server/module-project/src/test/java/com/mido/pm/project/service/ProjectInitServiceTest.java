package com.mido.pm.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.project.dto.InitiationFormDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectTemplateMapper;
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
 * 立项审批提交单测（mock 审批引擎/项目服务/Identity，无 DB）。
 */
@ExtendWith(MockitoExtension.class)
class ProjectInitServiceTest {

    @Mock private PmProjectMapper projectMapper;
    @Mock private PmProjectTemplateMapper templateMapper;
    @Mock private ProjectService projectService;
    @Mock private ApprovalService approvalService;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private IdentityProvider identityProvider;

    private ProjectInitService service;

    @BeforeEach
    void setUp() {
        service = new ProjectInitService(projectMapper, templateMapper, projectService,
                approvalService, approvalFlowService, identityProvider, new ObjectMapper());
    }

    private PmProject draft(String category, Long leaderId) {
        PmProject p = new PmProject();
        p.setId(1L);
        p.setStatus("草稿");
        p.setCategory(category);
        p.setLeaderId(leaderId);
        return p;
    }

    @Test
    void submitResolvesFlowSubmitsAndTransitionsToApproving() {
        PmProject p = draft("S", 1L);
        when(projectMapper.selectById(1L)).thenReturn(p);
        when(approvalFlowService.resolveFlowId("S_STANDARD")).thenReturn(99L);
        UserPrincipal leader = new UserPrincipal();
        leader.setJobLevel("L3");
        when(identityProvider.loadById(1L)).thenReturn(Optional.of(leader));
        when(approvalService.submit(any(SubmitDTO.class))).thenReturn(500L);

        Long instanceId = service.submitApproval(1L,
                new InitiationFormDTO("项目目标", null, null, "干系人初稿", "价值假设"));

        assertEquals(500L, instanceId);
        verify(approvalService).submit(any(SubmitDTO.class));
        verify(projectService).transition(eq(1L),
                argThat((ProjectTransitionDTO dto) -> "审批中".equals(dto.targetStatus())));
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
