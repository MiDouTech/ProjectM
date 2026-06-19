package com.mido.pm.change.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 变更单（pm_change_request）。受控变更台账：biz_type+biz_id 指向被改实体，before/after 为快照(JSON)。
 * status：draft/pending/approved/applied/rejected/withdrawn。
 */
@TableName("pm_change_request")
public class PmChangeRequest extends BaseEntity {

    private String bizType;
    private Long bizId;
    private String changeType;
    private String title;
    private String reason;
    private String impact;
    private String beforeSnapshot;
    private String afterPayload;
    private String status;
    private Long approvalInstanceId;
    private LocalDateTime appliedAt;

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterPayload() { return afterPayload; }
    public void setAfterPayload(String afterPayload) { this.afterPayload = afterPayload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApprovalInstanceId() { return approvalInstanceId; }
    public void setApprovalInstanceId(Long approvalInstanceId) { this.approvalInstanceId = approvalInstanceId; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
