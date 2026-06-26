package com.mido.pm.view.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 页面表单配置（pm_page_config，ADR-0004 · L3）。每租户每 (target, templateType) 一行。
 * config 为字段编排 JSON（layout + fields[]）；空配置由前端回落默认（内置字段全显示）。
 */
@TableName("pm_page_config")
public class PmPageConfig extends BaseEntity {

    private String target;
    private String templateType;
    private String config;

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
