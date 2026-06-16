package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目成员（pm_project_member）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project_member")
public class PmProjectMember extends BaseEntity {

    private Long projectId;
    private Long userId;
    /** 管理员/普通成员/只读成员 */
    private String projectRole;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProjectRole() { return projectRole; }
    public void setProjectRole(String projectRole) { this.projectRole = projectRole; }
}
