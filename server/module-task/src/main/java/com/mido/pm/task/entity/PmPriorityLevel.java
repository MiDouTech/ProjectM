package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 优先级档位（pm_priority_level）。level_value 越小优先级越高。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_priority_level")
public class PmPriorityLevel extends BaseEntity {

    private Long modeId;
    private String name;
    private String color;
    private Integer levelValue;
    private Integer sort;

    public Long getModeId() { return modeId; }
    public void setModeId(Long modeId) { this.modeId = modeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getLevelValue() { return levelValue; }
    public void setLevelValue(Integer levelValue) { this.levelValue = levelValue; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
