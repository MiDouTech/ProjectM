package com.mido.pm.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 审批节点待办/动作（approval_task）。action：approve/reject/transfer，null 表示待办未处理。
 */
@TableName("approval_task")
public class ApprovalTask extends BaseEntity {

    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_TRANSFER = "transfer";

    private Long instanceId;
    private String node;
    private Long approverId;
    private String action;
    private String comment;
    private LocalDateTime actedAt;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getNode() { return node; }
    public void setNode(String node) { this.node = node; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getActedAt() { return actedAt; }
    public void setActedAt(LocalDateTime actedAt) { this.actedAt = actedAt; }
}
