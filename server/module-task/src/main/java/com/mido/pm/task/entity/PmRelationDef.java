package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 工作项关联定义（pm_relation_def）：源类型↔目标类型 的关系语义（related/derived）。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_relation_def")
public class PmRelationDef extends BaseEntity {

    private Long sourceTypeId;
    private Long targetTypeId;
    private String relationKind;
    private String name;

    public Long getSourceTypeId() { return sourceTypeId; }
    public void setSourceTypeId(Long sourceTypeId) { this.sourceTypeId = sourceTypeId; }
    public Long getTargetTypeId() { return targetTypeId; }
    public void setTargetTypeId(Long targetTypeId) { this.targetTypeId = targetTypeId; }
    public String getRelationKind() { return relationKind; }
    public void setRelationKind(String relationKind) { this.relationKind = relationKind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
