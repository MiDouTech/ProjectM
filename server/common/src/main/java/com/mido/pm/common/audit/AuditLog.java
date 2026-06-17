package com.mido.pm.common.audit;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志（活动流）实体，对应 docs/data-model.md 的 sys_audit_log。
 * 平台基础表，不套用业务公共字段（无 update_by/is_deleted）；tenant_id 由拦截器注入。
 * 约定：target=实体类型（project/task），targetId=实体主键，action 取自 {@link AuditActions}，
 * detail 为可读变更明细 JSON（如 {from,to} 或 {changes:[{field,from,to}]}）。
 */
@TableName("sys_audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 操作人用户 ID（谁） */
    private Long userId;

    /** 审计动作码（取自 AuditActions，非领域事件名） */
    private String action;

    /** 被审计实体类型：project / task */
    private String target;

    /** 被审计实体主键 */
    private Long targetId;

    /** 变更明细（JSON 字符串） */
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
