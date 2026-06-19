package com.mido.pm.change.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 变更审批策略（pm_change_policy）。按 change_type 可配：是否必审 + 绑定审批流。
 */
@TableName("pm_change_policy")
public class PmChangePolicy extends BaseEntity {

    private String changeType;
    private Integer requireApproval;
    private Long flowId;
    private Integer enabled;

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Integer getRequireApproval() { return requireApproval; }
    public void setRequireApproval(Integer requireApproval) { this.requireApproval = requireApproval; }
    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
