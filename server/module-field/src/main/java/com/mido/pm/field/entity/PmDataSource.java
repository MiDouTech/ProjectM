package com.mido.pm.field.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 数据源（pm_data_source）：可复用的下拉选项集。下拉字段引用本表获得选项。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_data_source")
public class PmDataSource extends BaseEntity {

    private String name;
    private String groupName;
    private String remark;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
