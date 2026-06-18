package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * NPSS 单干系人评分（pm_npss_score）。评分待办先以 score=null 建好，干系人提交后回填。
 * weight 为快照（取自 pm_stakeholder.npss_weight，避免后续权重变更影响已发起轮次）。
 */
@TableName("pm_npss_score")
public class PmNpssScore extends BaseEntity {

    private Long reviewId;
    private Long stakeholderId;
    private Integer score;
    private BigDecimal weight;
    private String comment;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getStakeholderId() { return stakeholderId; }
    public void setStakeholderId(Long stakeholderId) { this.stakeholderId = stakeholderId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
