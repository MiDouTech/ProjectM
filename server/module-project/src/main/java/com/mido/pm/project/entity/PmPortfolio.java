package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目集（pm_portfolio）：项目的逻辑分组容器，供管理层做跨部门总览。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_portfolio")
public class PmPortfolio extends BaseEntity {

    private String name;
    private String description;
    private Long ownerId;
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
