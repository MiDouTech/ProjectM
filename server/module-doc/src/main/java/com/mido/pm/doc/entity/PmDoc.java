package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目知识库文档节点（pm_doc）。目录树：parent_id 构成层级，type=folder/doc。
 * 正文不在本表，存 pm_doc_version；current_version_id 指向最新版本。
 */
@TableName("pm_doc")
public class PmDoc extends BaseEntity {

    /** 文档目录节点类型 */
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_DOC = "doc";

    private Long projectId;
    private Long parentId;
    private String type;
    private String title;
    private String icon;
    private Integer sortNo;
    private Long currentVersionId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
}
