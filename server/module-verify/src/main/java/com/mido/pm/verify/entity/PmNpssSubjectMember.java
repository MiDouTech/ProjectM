package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * NPSS 评价主体成员（pm_npss_subject_member）。成员即干系人（stakeholderId 指向 pm_stakeholder.id）。
 * 同一主体可多人，汇总时这些成员的评分先取平均再按主体权重加权（npss-rule §3）。
 */
@TableName("pm_npss_subject_member")
public class PmNpssSubjectMember extends BaseEntity {

    private Long subjectId;
    private Long stakeholderId;

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Long getStakeholderId() { return stakeholderId; }
    public void setStakeholderId(Long stakeholderId) { this.stakeholderId = stakeholderId; }
}
