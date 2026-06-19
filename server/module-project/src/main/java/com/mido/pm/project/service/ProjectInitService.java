package com.mido.pm.project.service;

import com.mido.pm.approval.dto.InstanceVO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.InitiationFormDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 立项审批业务：基于通用审批引擎，按「项目类型」绑定的默认审批流提交立项申请，并驱动项目 草稿→审批中。
 * 审批流由项目类型属性 default_flow_id 决定（取代原模板 config.approvalFlow 字符串解析 + 类型 if-else 兜底）。
 * 审批全流程通过后由 {@link com.mido.pm.project.event.ProjectApprovalListener} 监听 approval.approved
 * 驱动 审批中→已注册（事件解耦，审批不直写项目表）。
 */
@Service
public class ProjectInitService {

    public static final String BIZ_TYPE = "project_init";

    private final PmProjectMapper projectMapper;
    private final ProjectService projectService;
    private final ApprovalService approvalService;
    private final IdentityProvider identityProvider;
    private final ProjectTypeResolver projectTypeResolver;

    public ProjectInitService(PmProjectMapper projectMapper, ProjectService projectService,
                              ApprovalService approvalService, IdentityProvider identityProvider,
                              ProjectTypeResolver projectTypeResolver) {
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.approvalService = approvalService;
        this.identityProvider = identityProvider;
        this.projectTypeResolver = projectTypeResolver;
    }

    /** 提交立项审批：解析类型→其绑定的默认流 → 提交审批引擎（首节点职级 guard）→ 项目 草稿→审批中。返回审批实例 ID。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submitApproval(Long projectId, InitiationFormDTO form) {
        PmProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿态项目可提交立项审批");
        }

        // 申请单可覆盖 Leader/预算；改 Leader 同步归属部门(dept_id)，与 ProjectService 一致，避免数据范围 ACL 错位
        if (form.leaderId() != null) {
            project.setLeaderId(form.leaderId());
            project.setDeptId(identityProvider.loadById(form.leaderId())
                    .map(u -> u.getDeptId()).orElse(project.getDeptId()));
        }
        BigDecimal budget = form.budget() != null ? form.budget() : project.getBudget();
        if (form.leaderId() != null || form.budget() != null) {
            project.setBudget(budget);
            projectMapper.updateById(project);
        }

        // 项目类型 → 绑定的默认审批流 + 职级门槛（去硬编码：均读类型属性）
        PmProjectType type = projectTypeResolver.require(project.getCategory(), project.getSubCategory());
        Long flowId = type.getDefaultFlowId();
        if (flowId == null) {
            throw new BizException(ErrorCode.CONFLICT, "项目类型「" + type.getName() + "」未绑定审批流");
        }
        String leaderJobLevel = project.getLeaderId() == null ? null
                : identityProvider.loadById(project.getLeaderId()).map(u -> u.getJobLevel()).orElse(null);

        Map<String, Object> formData = new LinkedHashMap<>();
        // 业务展示快照：供审批「待我审批」列表与详情直接展示项目名/编号，无需审批侧反查项目
        formData.put("projectName", project.getName());
        formData.put("projectCode", project.getCode());
        formData.put("objective", form.objective());
        formData.put("valueHypothesis", form.valueHypothesis());
        formData.put("stakeholderDraft", form.stakeholderDraft());
        formData.put("budget", budget);
        formData.put("leaderId", project.getLeaderId());
        // 路由/guard 上下文
        formData.put("category", project.getCategory());
        formData.put("amount", budget);
        formData.put("jobLevel", leaderJobLevel);
        formData.put("minJobLevel", type.getMinJobLevel());

        Long instanceId = approvalService.submit(new SubmitDTO(flowId, BIZ_TYPE, projectId, formData));
        projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.APPROVING.getCode(), null));
        return instanceId;
    }

    /** 当前立项审批实例（含「待谁审批」），未提交过则返回 null。 */
    public InstanceVO currentApproval(Long projectId) {
        return approvalService.findCurrentInstance(BIZ_TYPE, projectId);
    }
}
