package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * NPSS 价值验收轮次（pm_npss_review）。status：pending 评分中 / done 已汇总。
 * weighted_score/result_level 由 {@link com.mido.pm.verify.domain.NpssCalculator} 汇总写入。
 */
@TableName("pm_npss_review")
public class PmNpssReview extends BaseEntity {

    private Long projectId;
    private String round;
    private String status;
    private BigDecimal weightedScore;
    private String resultLevel;
    private LocalDateTime reviewedAt;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getWeightedScore() { return weightedScore; }
    public void setWeightedScore(BigDecimal weightedScore) { this.weightedScore = weightedScore; }
    public String getResultLevel() { return resultLevel; }
    public void setResultLevel(String resultLevel) { this.resultLevel = resultLevel; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
