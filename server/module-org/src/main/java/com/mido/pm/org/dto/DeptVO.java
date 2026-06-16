package com.mido.pm.org.dto;

import java.util.ArrayList;
import java.util.List;

/** 部门树节点视图。children 用于树形装配。 */
public class DeptVO {
    private Long id;
    private String name;
    private Long parentId;
    private List<DeptVO> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public List<DeptVO> getChildren() { return children; }
    public void setChildren(List<DeptVO> children) { this.children = children; }
}
