package com.mido.pm.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDate;

/** 简报跟进问题（pm_briefing_issue）。status：open/following/closed。 */
@TableName("pm_briefing_issue")
public class PmBriefingIssue extends BaseEntity {

    private Long briefingId;
    private Long raisedBy;
    private Long ownerId;
    private String content;
    private String status;
    private LocalDate dueDate;

    public Long getBriefingId() { return briefingId; }
    public void setBriefingId(Long briefingId) { this.briefingId = briefingId; }
    public Long getRaisedBy() { return raisedBy; }
    public void setRaisedBy(Long raisedBy) { this.raisedBy = raisedBy; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
