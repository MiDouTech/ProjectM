package com.mido.pm.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.InitiationFormDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.entity.PmProjectTemplate;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectTemplateMapper;
import com.mido.pm.project.template.TemplateConfig;
import com.mido.pm.provider.identity.IdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 立项审批业务：基于通用审批引擎，按模板默认审批流提交立项申请，并驱动项目 草稿→审批中。
 * 审批全流程通过后由 {@link com.mido.pm.project.event.ProjectApprovalListener} 监听 approval.approved
 * 驱动 审批中→已注册（事件解耦，审批不直写项目表）。
 */
@Service
public class ProjectInitService {

    public static final String BIZ_TYPE = "project_init";

    private final PmProjectMapper projectMapper;
    private final PmProjectTemplateMapper templateMapper;
    private final ProjectService projectService;
    private final ApprovalService approvalService;
    private final ApprovalFlowService approvalFlowService;
    private final IdentityProvider identityProvider;
    private final ObjectMapper objectMapper;

    public ProjectInitService(PmProjectMapper projectMapper, PmProjectTemplateMapper templateMapper,
                              ProjectService projectService, ApprovalService approvalService,
                              ApprovalFlowService approvalFlowService, IdentityProvider identityProvider,
                              ObjectMapper objectMapper) {
        this.projectMapper = projectMapper;
        this.templateMapper = templateMapper;
        this.projectService = projectService;
        this.approvalService = approvalService;
        this.approvalFlowService = approvalFlowService;
        this.identityProvider = identityProvider;
        this.objectMapper = objectMapper;
    }

    /** 提交立项审批：解析默认流 → 提交审批引擎（首节点职级 guard）→ 项目 草稿→审批中。返回审批实例 ID。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submitApproval(Long projectId, InitiationFormDTO form) {
        PmProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿态项目可提交立项审批");
        }

        // 申请单可覆盖 Leader/预算
        if (form.leaderId() != null) {
            project.setLeaderId(form.leaderId());
        }
        BigDecimal budget = form.budget() != null ? form.budget() : project.getBudget();
        if (form.leaderId() != null || form.budget() != null) {
            project.setBudget(budget);
            projectMapper.updateById(project);
        }

        String flowKey = resolveFlowKey(project);
        Long flowId = approvalFlowService.resolveFlowId(flowKey);
        String leaderJobLevel = project.getLeaderId() == null ? null
                : identityProvider.loadById(project.getLeaderId()).map(u -> u.getJobLevel()).orElse(null);

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("objective", form.objective());
        formData.put("valueHypothesis", form.valueHypothesis());
        formData.put("stakeholderDraft", form.stakeholderDraft());
        formData.put("budget", budget);
        formData.put("leaderId", project.getLeaderId());
        // 路由/guard 上下文
        formData.put("category", project.getCategory());
        formData.put("amount", budget);
        formData.put("jobLevel", leaderJobLevel);

        Long instanceId = approvalService.submit(new SubmitDTO(flowId, BIZ_TYPE, projectId, formData));
        projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.APPROVING.getCode(), null));
        return instanceId;
    }

    /** 解析默认审批流标识：优先模板 config.approvalFlow，否则按类型/子类回落。 */
    private String resolveFlowKey(PmProject project) {
        if (project.getTemplateId() != null) {
            PmProjectTemplate template = templateMapper.selectById(project.getTemplateId());
            if (template != null && template.getConfig() != null) {
                try {
                    String key = objectMapper.readValue(template.getConfig(), TemplateConfig.class).approvalFlow();
                    if (key != null && !key.isBlank()) {
                        return key;
                    }
                } catch (Exception ignored) {
                    // 解析失败回落默认
                }
            }
        }
        return defaultFlowKey(project);
    }

    private String defaultFlowKey(PmProject project) {
        String category = project.getCategory();
        if ("S".equals(category)) {
            return "S_STANDARD";
        }
        if ("I".equals(category)) {
            return "I_POC";
        }
        // O
        String sub = project.getSubCategory();
        if ("定向整改".equals(sub)) {
            return "O_RECTIFY";
        }
        if ("专项督办".equals(sub)) {
            return "O_SUPERVISE";
        }
        return "O_NORMAL";
    }
}
