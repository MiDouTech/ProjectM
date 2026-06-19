package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 文档模板（pm_doc_template）：content 为 Tiptap JSON。 */
@TableName("pm_doc_template")
public class PmDocTemplate extends BaseEntity {
    private String name;
    private String content;
    private Integer sortNo;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
