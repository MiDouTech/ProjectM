package com.mido.pm.field.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 数据源选项（pm_data_source_option）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_data_source_option")
public class PmDataSourceOption extends BaseEntity {

    private Long dataSourceId;
    private String value;
    private String label;
    private Integer sortNo;

    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
