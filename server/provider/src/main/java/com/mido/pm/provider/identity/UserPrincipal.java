package com.mido.pm.provider.identity;

import com.mido.pm.common.security.DataScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 身份主体：认证与鉴权所需的用户快照，由 {@link IdentityProvider} 装配。
 * 屏蔽底层身份源（本地 sys_user / 企微通讯录），业务/安全层只依赖本对象。
 */
public class UserPrincipal {

    private Long userId;
    private String username;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}
