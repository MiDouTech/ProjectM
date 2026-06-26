package com.mido.pm.project.provider;

import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import com.mido.pm.project.entity.PmComponent;
import com.mido.pm.project.entity.PmProjectRole;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.mapper.PmComponentMapper;
import com.mido.pm.project.mapper.PmProjectRoleMapper;
import com.mido.pm.project.mapper.PmProjectTypeMapper;
import org.springframework.stereotype.Component;

/**
 * 项目域租户播种（order=30，最后）：项目角色（管理员/普通成员/只读成员）、组件库（11 个内置组件目录）、
 * 内置项目类型 S/I/O*（绑定 order=20 审批域写入共享袋的对应默认审批流 id）。与 V54/V62/V17/V19 内置种子等价。
 */
@Component
public class ProjectTenantProvisioner implements TenantProvisioner {

    private final PmProjectRoleMapper roleMapper;
    private final PmComponentMapper componentMapper;
    private final PmProjectTypeMapper typeMapper;

    public ProjectTenantProvisioner(PmProjectRoleMapper roleMapper, PmComponentMapper componentMapper,
                                    PmProjectTypeMapper typeMapper) {
        this.roleMapper = roleMapper;
        this.componentMapper = componentMapper;
        this.typeMapper = typeMapper;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void provision(TenantProvisionContext ctx) {
        projectRole("管理员", 10);
        projectRole("普通成员", 20);
        projectRole("只读成员", 30);

        component("overview", "概览", "Odometer", 0, 10);
        component("approval", "立项", "Stamp", 0, 20);
        component("info", "信息", "InfoFilled", 0, 30);
        component("task", "任务", "Tickets", 1, 40);
        component("goal", "目标", "Aim", 0, 50);
        component("stakeholder", "干系人", "User", 0, 60);
        component("verify", "验收", "CircleCheck", 0, 70);
        component("gantt", "甘特图", "TrendCharts", 1, 80);
        component("cost", "费用", "Money", 0, 90);
        component("doc", "文件", "Folder", 0, 100);
        component("activity", "活动", "Clock", 0, 110);

        projectType("S", "战略级", null, "danger", 10, "L3", 1, "S_STANDARD", 1, ctx, "IMP/MAP/EBC 等年度重点");
        projectType("I", "创新级", null, "warning", 20, null, 1, "I_POC", 0, ctx, "一米宽十米深探索/MTS 类");
        projectType("O_NORMAL", "运营级·常规运营", "O", "primary", 30, "L2", 1, "O_NORMAL", 0, ctx, "米多星球/PDA 改造等攻坚");
        projectType("O_RECTIFY", "运营级·定向整改", "O", "info", 40, "L2", 0, "O_RECTIFY", 0, ctx, "部门月度问题及对策转化");
        projectType("O_SUPERVISE", "运营级·专项督办", "O", "info", 50, "L2", 0, "O_SUPERVISE", 0, ctx, "管委会基础素养处分转化");
    }

    private void projectRole(String name, int sort) {
        PmProjectRole r = new PmProjectRole();
        r.setCode(name);
        r.setName(name);
        r.setBuiltin(1);
        r.setSort(sort);
        r.setStatus("active");
        roleMapper.insert(r);
    }

    private void component(String code, String name, String icon, int multiInstance, int sort) {
        PmComponent c = new PmComponent();
        c.setCode(code);
        c.setName(name);
        c.setIcon(icon);
        c.setMultiInstance(multiInstance);
        c.setBuiltin(1);
        c.setSort(sort);
        componentMapper.insert(c);
    }

    private void projectType(String code, String name, String parentCode, String color, int sort, String minJobLevel,
                             int requiresNpss, String flowName, int requireGoalAlignment,
                             TenantProvisionContext ctx, String description) {
        PmProjectType t = new PmProjectType();
        t.setCode(code);
        t.setName(name);
        t.setParentCode(parentCode);
        t.setColor(color);
        t.setSort(sort);
        t.setMinJobLevel(minJobLevel);
        t.setRequiresNpss(requiresNpss);
        t.setDefaultFlowId(ctx.get(TenantProvisionContext.KEY_FLOW_PREFIX + flowName));
        t.setRequireGoalAlignment(requireGoalAlignment);
        t.setStatus("active");
        t.setDescription(description);
        typeMapper.insert(t);
    }
}
