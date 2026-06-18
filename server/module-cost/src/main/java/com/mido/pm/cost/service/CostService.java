package com.mido.pm.cost.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.cost.domain.CostStatus;
import com.mido.pm.cost.dto.CostCreateDTO;
import com.mido.pm.cost.dto.CostUpdateDTO;
import com.mido.pm.cost.dto.CostVO;
import com.mido.pm.cost.entity.PmCost;
import com.mido.pm.cost.event.CostEvents;
import com.mido.pm.cost.mapper.PmCostMapper;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用服务：CRUD + 提报审批（复用 module-approval 引擎，biz_type=cost）+ 预算绑定预警。
 * 预算绑定：项目累计 actual &gt; 项目预算时发 cost.exceeded.budget 与 project.budget.exceeded，仅预警不阻断。
 */
@Service
public class CostService {

    /** 费用审批流标识（见 V7 种子） */
    public static final String COST_FLOW = "COST_DEFAULT";
    /** 审批 biz_type */
    public static final String BIZ_TYPE = "cost";

    private final PmCostMapper costMapper;
    private final ProjectService projectService;
    private final ApprovalService approvalService;
    private final ApprovalFlowService approvalFlowService;
    private final DomainEventPublisher eventPublisher;

    public CostService(PmCostMapper costMapper, ProjectService projectService,
                       ApprovalService approvalService, ApprovalFlowService approvalFlowService,
                       DomainEventPublisher eventPublisher) {
        this.costMapper = costMapper;
        this.projectService = projectService;
        this.approvalService = approvalService;
        this.approvalFlowService = approvalFlowService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CostCreateDTO dto) {
        PmCost c = new PmCost();
        c.setProjectId(dto.projectId());
        c.setTitle(dto.title());
        c.setAccount(dto.account());
        c.setBudgetAmount(dto.budgetAmount());
        c.setActualAmount(dto.actualAmount());
        c.setOccurDate(dto.occurDate());
        c.setPayDate(dto.payDate());
        c.setStatus(CostStatus.NOT_OCCURRED);
        costMapper.insert(c);
        checkBudget(c.getProjectId(), c.getId());
        return c.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CostUpdateDTO dto) {
        PmCost c = requireExists(id);
        c.setTitle(dto.title());
        c.setAccount(dto.account());
        c.setBudgetAmount(dto.budgetAmount());
        c.setActualAmount(dto.actualAmount());
        c.setOccurDate(dto.occurDate());
        c.setPayDate(dto.payDate());
        costMapper.updateById(c);
        checkBudget(c.getProjectId(), id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        costMapper.deleteById(id); // 逻辑删除（@TableLogic）
    }

    public List<CostVO> listByProject(Long projectId) {
        return costMapper.selectList(Wrappers.<PmCost>lambdaQuery()
                        .eq(PmCost::getProjectId, projectId)
                        .orderByDesc(PmCost::getId))
                .stream().map(this::toVO).toList();
    }

    public CostVO get(Long id) {
        return toVO(requireExists(id));
    }

    /** 提报审批：发起 cost 审批实例，存 approval_id，发 cost.submitted。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long id) {
        PmCost c = requireExists(id);
        Long flowId = approvalFlowService.resolveFlowId(COST_FLOW);
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("title", c.getTitle());
        formData.put("account", c.getAccount());
        formData.put("amount", c.getBudgetAmount());
        Long instanceId = approvalService.submit(new SubmitDTO(flowId, BIZ_TYPE, id, formData));
        c.setApprovalId(instanceId);
        costMapper.updateById(c);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("costId", id);
        payload.put("projectId", c.getProjectId());
        payload.put("approvalId", instanceId);
        payload.put("amount", c.getBudgetAmount());
        payload.put("applicantId", UserContext.currentUserId());
        eventPublisher.publish(CostEvents.SUBMITTED, payload);
        return instanceId;
    }

    /** 审批结果回写费用状态（由 CostApprovalListener 调用）：通过→已发生，驳回→被退回。 */
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovalResult(Long costId, boolean approved) {
        PmCost c = costMapper.selectById(costId);
        if (c == null) {
            return;
        }
        c.setStatus(approved ? CostStatus.OCCURRED : CostStatus.RETURNED);
        costMapper.updateById(c);
    }

    /**
     * 预算绑定检查：项目累计实际成本 &gt; 项目预算 → 发 cost.exceeded.budget 与 project.budget.exceeded（不阻断）。
     */
    private void checkBudget(Long projectId, Long costId) {
        ProjectVO project = projectService.get(projectId);
        BigDecimal budget = project == null ? null : project.budget();
        if (budget == null) {
            return; // 未设预算则不预警
        }
        BigDecimal cumulative = costMapper.selectList(Wrappers.<PmCost>lambdaQuery()
                        .eq(PmCost::getProjectId, projectId))
                .stream().map(c -> nz(c.getActualAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (cumulative.compareTo(budget) <= 0) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", projectId);
        payload.put("costId", costId);
        payload.put("cumulativeActual", cumulative);
        payload.put("budget", budget);
        payload.put("leaderId", project.leaderId()); // 预警收件人=项目负责人，监听器据此通知
        eventPublisher.publish(CostEvents.EXCEEDED_BUDGET, payload);
        eventPublisher.publish(CostEvents.PROJECT_BUDGET_EXCEEDED, payload);
    }

    private PmCost requireExists(Long id) {
        PmCost c = costMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "费用记录不存在");
        }
        return c;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private CostVO toVO(PmCost c) {
        return new CostVO(c.getId(), c.getProjectId(), c.getTitle(), c.getAccount(),
                c.getBudgetAmount(), c.getActualAmount(), c.getOccurDate(), c.getPayDate(),
                c.getStatus(), c.getApprovalId(), c.getCreateTime());
    }
}
