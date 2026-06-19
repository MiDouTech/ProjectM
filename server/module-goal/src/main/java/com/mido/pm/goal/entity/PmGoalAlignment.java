package com.mido.pm.goal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 目标对齐（pm_goal_alignment）：目标 ↔ project/task 的多对多弱关联。
 * ★弱关联，非父子：目标不挂执行树，仅记录"对齐到哪个 project/task"。
 */
@TableName("pm_goal_alignment")
public class PmGoalAlignment extends BaseEntity {

    private Long goalId;
    /** 对齐目标类型：project / task */
    private String targetType;
    private Long targetId;
    /** 对齐贡献权重（加权汇总用，默认 1） */
    private java.math.BigDecimal weight;

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public java.math.BigDecimal getWeight() { return weight; }
    public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }
}
