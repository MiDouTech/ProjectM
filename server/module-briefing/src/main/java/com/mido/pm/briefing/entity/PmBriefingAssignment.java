package com.mido.pm.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 简报模板指派（pm_briefing_assignment）。target_type：user/dept。 */
@TableName("pm_briefing_assignment")
public class PmBriefingAssignment extends BaseEntity {

    private Long templateId;
    private String targetType;
    private Long targetId;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
}
