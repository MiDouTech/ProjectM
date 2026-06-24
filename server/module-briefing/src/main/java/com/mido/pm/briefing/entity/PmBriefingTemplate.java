package com.mido.pm.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 简报模板（pm_briefing_template）。type：daily/weekly/monthly；schemaDef 为字段定义 JSON 数组
 * （列名 schema_def，避开 MySQL 8 保留字 schema）。
 */
@TableName("pm_briefing_template")
public class PmBriefingTemplate extends BaseEntity {

    private String name;
    private String type;
    private String schemaDef;
    private String scope;
    private Integer isBuiltin;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSchemaDef() { return schemaDef; }
    public void setSchemaDef(String schemaDef) { this.schemaDef = schemaDef; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Integer getIsBuiltin() { return isBuiltin; }
    public void setIsBuiltin(Integer isBuiltin) { this.isBuiltin = isBuiltin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
