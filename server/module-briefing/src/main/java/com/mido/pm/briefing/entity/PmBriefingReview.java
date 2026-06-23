package com.mido.pm.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/** 简报评审批注（pm_briefing_review）。action：comment 批注 / approve 已阅。 */
@TableName("pm_briefing_review")
public class PmBriefingReview extends BaseEntity {

    private Long briefingId;
    private Long reviewerId;
    private String action;
    private String comment;
    private LocalDateTime reviewedAt;

    public Long getBriefingId() { return briefingId; }
    public void setBriefingId(Long briefingId) { this.briefingId = briefingId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
