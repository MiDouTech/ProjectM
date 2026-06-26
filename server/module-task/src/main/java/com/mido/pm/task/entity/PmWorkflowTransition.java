package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 工作流状态转移（pm_workflow_transition）：某类型下允许 from_status → to_status 的一条转移。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_workflow_transition")
public class PmWorkflowTransition extends BaseEntity {

    private Long typeId;
    private Long fromStatusId;
    private Long toStatusId;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public Long getFromStatusId() { return fromStatusId; }
    public void setFromStatusId(Long fromStatusId) { this.fromStatusId = fromStatusId; }
    public Long getToStatusId() { return toStatusId; }
    public void setToStatusId(Long toStatusId) { this.toStatusId = toStatusId; }
}
