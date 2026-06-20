package com.mido.pm.common.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 当前登录用户的安全上下文载体。由组织域（module-org）在认证/加载用户时填充并写入 {@link UserContext}。
 * 持有数据范围注入所需的信息：用户 ID、所属部门、下属部门、自定义部门集、各资源的有效数据范围。
 *
 * <p>common 不依赖 org：scope 的解析（读 sys_role_data_scope、合并多角色取最宽）在 org 完成，
 * 结果以本对象形式推入上下文，common 的拦截器只消费、不查表。</p>
 */
public class CurrentUser {

    private Long userId;
    private Long deptId;
    /** 模拟登录来源：发起模拟的平台运营账号 ID；非模拟登录为 null（只读拦截据此判定） */
    private Long impersonatedBy;
    /** 下属部门 ID（不含本部门），用于 dept_and_sub */
    private List<Long> subDeptIds = new ArrayList<>();
    /** 自定义部门集，用于 custom（持久化存储待后续迁移补充） */
    private List<Long> customDeptIds = new ArrayList<>();
    /** 资源 → 有效数据范围（已按多角色合并取最宽） */
    private Map<String, DataScope> resourceScopes = Collections.emptyMap();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getImpersonatedBy() {
        return impersonatedBy;
    }

    public void setImpersonatedBy(Long impersonatedBy) {
        this.impersonatedBy = impersonatedBy;
    }

    /** 当前会话是否为平台运营的模拟登录。 */
    public boolean isImpersonating() {
        return impersonatedBy != null;
    }

    public List<Long> getSubDeptIds() {
        return subDeptIds;
    }

    public void setSubDeptIds(List<Long> subDeptIds) {
        this.subDeptIds = subDeptIds == null ? new ArrayList<>() : subDeptIds;
    }

    public List<Long> getCustomDeptIds() {
        return customDeptIds;
    }

    public void setCustomDeptIds(List<Long> customDeptIds) {
        this.customDeptIds = customDeptIds == null ? new ArrayList<>() : customDeptIds;
    }

    public Map<String, DataScope> getResourceScopes() {
        return resourceScopes;
    }

    public void setResourceScopes(Map<String, DataScope> resourceScopes) {
        this.resourceScopes = resourceScopes == null ? Collections.emptyMap() : resourceScopes;
    }

    /** 某资源的有效数据范围；未配置则视为 ALL（数据范围是 opt-in 收紧规则，缺省不限制）。 */
    public DataScope effectiveScope(String resource) {
        return resourceScopes.getOrDefault(resource, DataScope.ALL);
    }

    /** 本部门 + 下属部门 ID 集（dept_and_sub 用）。 */
    public List<Long> deptAndSubIds() {
        List<Long> ids = new ArrayList<>();
        if (deptId != null) {
            ids.add(deptId);
        }
        ids.addAll(subDeptIds);
        return ids;
    }
}
