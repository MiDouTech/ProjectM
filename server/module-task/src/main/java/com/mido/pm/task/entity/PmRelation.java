package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 工作项关联实例（pm_relation）：source_task → target_task，kind=related/derived。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_relation")
public class PmRelation extends BaseEntity {

    private String relationKind;
    private Long sourceTaskId;
    private Long targetTaskId;

    public String getRelationKind() { return relationKind; }
    public void setRelationKind(String relationKind) { this.relationKind = relationKind; }
    public Long getSourceTaskId() { return sourceTaskId; }
    public void setSourceTaskId(Long sourceTaskId) { this.sourceTaskId = sourceTaskId; }
    public Long getTargetTaskId() { return targetTaskId; }
    public void setTargetTaskId(Long targetTaskId) { this.targetTaskId = targetTaskId; }
}
