package com.mido.pm.provider.identity;

import com.mido.pm.common.security.DataScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 身份主体：认证与鉴权所需的用户快照，由 {@link IdentityProvider} 装配。
 * 屏蔽底层身份源（本地 sys_user / 企微通讯录），业务/安全层只依赖本对象。
 */
public class UserPrincipal {

    private Long userId;
    /** 所属租户 ID（多租户登录隔离据此签发令牌租户声明） */
    private Long tenantId;
    private String username;
    /** 手机号：登录账号（租户内唯一） */
    private String phone;
    private String name;
    /** 密码哈希（仅 loadByUsername 用于登录校验时填充） */
    private String passwordHash;
    private Long deptId;
    private String jobLevel;
    private String status;
    /** 权限码集合（用于 @PreAuthorize 鉴权） */
    private List<String> permCodes = new ArrayList<>();
    /** 下属部门 ID（dept_and_sub 数据范围用） */
    private List<Long> subDeptIds = new ArrayList<>();
    /** 自定义部门集（custom 数据范围用） */
    private List<Long> customDeptIds = new ArrayList<>();
    /** 资源 → 有效数据范围（已合并多角色取最宽） */
    private Map<String, DataScope> resourceScopes = Collections.emptyMap();
    /** 仅查看字段键集合（"resource.field"），已合并多角色取最宽 */
    private Set<String> viewOnlyFields = new HashSet<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getJobLevel() {
        return jobLevel;
    }

    public void setJobLevel(String jobLevel) {
        this.jobLevel = jobLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getPermCodes() {
        return permCodes;
    }

    public void setPermCodes(List<String> permCodes) {
        this.permCodes = permCodes == null ? new ArrayList<>() : permCodes;
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

    public Set<String> getViewOnlyFields() {
        return viewOnlyFields;
    }

    public void setViewOnlyFields(Set<String> viewOnlyFields) {
        this.viewOnlyFields = viewOnlyFields == null ? new HashSet<>() : viewOnlyFields;
    }
}
