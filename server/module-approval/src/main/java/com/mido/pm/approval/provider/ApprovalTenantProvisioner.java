package com.mido.pm.approval.provider;

import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 审批域租户播种（order=20）：为新租户建默认审批流——5 条立项流（对应内置项目类型 S/I/O*）+ 1 条费用流。
 * 新租户初始仅有管理员一人，故各流统一为单节点「审批」、审批人=管理员（开通即可立项审批，运营后续可在
 * 流程设计器重配多级审批人）。生成的 flowId 按流名写入共享袋，供项目类型 provisioner 绑定 defaultFlowId。
 */
@Component
public class ApprovalTenantProvisioner implements TenantProvisioner {

    /** 立项流名（与内置项目类型期望的默认流对齐）。 */
    private static final List<String> PROJECT_INIT_FLOWS =
            List.of("S_STANDARD", "I_POC", "O_NORMAL", "O_RECTIFY", "O_SUPERVISE");

    /** 内置流的中文展示名（面向用户，避免暴露 S_STANDARD 等技术编码；与 V22 回填命名一致）。 */
    private static final Map<String, String> FLOW_DISPLAY_NAMES = Map.of(
            "S_STANDARD", "战略级标准流程",
            "I_POC", "创新级 POC 流程",
            "O_NORMAL", "常规运营流程",
            "O_RECTIFY", "定向整改流程",
            "O_SUPERVISE", "专项督办流程",
            "COST_DEFAULT", "费用审批（默认）");

    private final ApprovalFlowMapper flowMapper;

    public ApprovalTenantProvisioner(ApprovalFlowMapper flowMapper) {
        this.flowMapper = flowMapper;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void provision(TenantProvisionContext ctx) {
        Long adminId = ctx.get(TenantProvisionContext.KEY_ADMIN_USER_ID);
        String def = singleApproverDefinition(adminId);
        for (String name : PROJECT_INIT_FLOWS) {
            seedFlow(ctx, name, "project_init", def);
        }
        seedFlow(ctx, "COST_DEFAULT", "cost", def);
    }

    private void seedFlow(TenantProvisionContext ctx, String name, String bizType, String definition) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setName(name);
        flow.setDisplayName(FLOW_DISPLAY_NAMES.getOrDefault(name, name));
        flow.setBizType(bizType);
        flow.setMode("fixed");
        flow.setDefinition(definition);
        flowMapper.insert(flow);
        ctx.put(TenantProvisionContext.KEY_FLOW_PREFIX + name, flow.getId());
    }

    /** 单节点「审批」、审批人=管理员（approverType 未指定即按 USER 型取 approvers）。 */
    private String singleApproverDefinition(Long adminId) {
        String approvers = adminId == null ? "[]" : "[" + adminId + "]";
        return "{\"nodes\":[{\"key\":\"approve\",\"name\":\"审批\",\"approvers\":" + approvers + ",\"mode\":\"or\"}]}";
    }
}
