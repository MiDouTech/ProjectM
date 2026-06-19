package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目类型（pm_project_type）。SaaS 租户可自配的一等实体，取代原硬编码枚举 S/I/O。
 * 把立项职级门槛 / 是否走 NPSS / 默认审批流 / 干系人权重模板收敛为类型的可配置属性。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project_type")
public class PmProjectType extends BaseEntity {

    /** 租户内唯一程序引用（如 S/I/O_NORMAL），禁与显示名混用 */
    private String code;
    private String name;
    /** 上级类型码（报表汇总用，如 O_NORMAL→O） */
    private String parentCode;
    /** design-system 颜色 token 名（如 danger/primary），禁裸 hex */
    private String color;
    private String icon;
    private Integer sort;
    /** 立项 Leader 最低职级门槛（如 L3），空=不限 */
    private String minJobLevel;
    /** 默认是否走 NPSS 价值验收：1 是 / 0 否 */
    private Integer requiresNpss;
    /** 绑定的默认审批流（approval_flow.id） */
    private Long defaultFlowId;
    /** 默认干系人权重模板（JSON） */
    private String stakeholderTpl;
    /** 状态：active/disabled */
    private String status;
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getMinJobLevel() { return minJobLevel; }
    public void setMinJobLevel(String minJobLevel) { this.minJobLevel = minJobLevel; }
    public Integer getRequiresNpss() { return requiresNpss; }
    public void setRequiresNpss(Integer requiresNpss) { this.requiresNpss = requiresNpss; }
    public Long getDefaultFlowId() { return defaultFlowId; }
    public void setDefaultFlowId(Long defaultFlowId) { this.defaultFlowId = defaultFlowId; }
    public String getStakeholderTpl() { return stakeholderTpl; }
    public void setStakeholderTpl(String stakeholderTpl) { this.stakeholderTpl = stakeholderTpl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
