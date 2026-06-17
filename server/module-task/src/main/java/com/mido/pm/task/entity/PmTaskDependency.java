package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 任务依赖（pm_task_dependency）。predecessor 前置 → successor 后继；type 默认 FS（前置完成后后继开始）。
 */
@TableName("pm_task_dependency")
public class PmTaskDependency extends BaseEntity {

    private Long predecessorId;
    private Long successorId;
    private String type;

    public Long getPredecessorId() { return predecessorId; }
    public void setPredecessorId(Long predecessorId) { this.predecessorId = predecessorId; }
    public Long getSuccessorId() { return successorId; }
    public void setSuccessorId(Long successorId) { this.successorId = successorId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
