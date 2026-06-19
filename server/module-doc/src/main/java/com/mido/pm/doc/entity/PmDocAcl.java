package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 文档级授权（pm_doc_acl）：principal(user/role) × permission(read/write/admin)。 */
@TableName("pm_doc_acl")
public class PmDocAcl extends BaseEntity {
    public static final String P_USER = "user";
    public static final String P_ROLE = "role";

    private Long docId;
    private String principalType;
    private Long principalId;
    private String permission;

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public String getPrincipalType() { return principalType; }
    public void setPrincipalType(String principalType) { this.principalType = principalType; }
    public Long getPrincipalId() { return principalId; }
    public void setPrincipalId(Long principalId) { this.principalId = principalId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
