package com.mido.pm.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 简报评审人/抄送（pm_briefing_recipient）。type：reviewer/cc。 */
@TableName("pm_briefing_recipient")
public class PmBriefingRecipient extends BaseEntity {

    private Long briefingId;
    private Long userId;
    private String type;

    public Long getBriefingId() { return briefingId; }
    public void setBriefingId(Long briefingId) { this.briefingId = briefingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
