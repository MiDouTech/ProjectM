package com.mido.pm.common.tenant;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户开通播种上下文：承载新租户基本信息 + 管理员初始凭据 + 跨 provisioner 共享 id 袋。
 * 共享袋用于把前序域生成的 id 传给后序域（如 approval 写入 {@code flow:S_STANDARD}→id，
 * project 读出绑定到项目类型 defaultFlowId），避免跨域直接查表。
 */
public final class TenantProvisionContext {

    /** 共享袋约定键：组织管理员用户 id / 管理员角色 id。 */
    public static final String KEY_ADMIN_USER_ID = "admin.userId";
    public static final String KEY_ADMIN_ROLE_ID = "admin.roleId";
    /** 审批流共享键前缀：{@code flow:<bizType>} → flowId（项目类型据此绑定默认审批流）。 */
    public static final String KEY_FLOW_PREFIX = "flow:";

    private final Long tenantId;
    private final String tenantCode;
    private final String tenantName;
    private final String adminUsername;
    private final String adminPassword;
    private final Map<String, Long> shared = new HashMap<>();

    public TenantProvisionContext(Long tenantId, String tenantCode, String tenantName,
                                  String adminUsername, String adminPassword) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public Long tenantId() { return tenantId; }
    public String tenantCode() { return tenantCode; }
    public String tenantName() { return tenantName; }
    public String adminUsername() { return adminUsername; }
    public String adminPassword() { return adminPassword; }

    /** 写入共享 id（供后序 provisioner 读取）。 */
    public void put(String key, Long value) { shared.put(key, value); }

    /** 读取共享 id；不存在返回 null。 */
    public Long get(String key) { return shared.get(key); }
}
