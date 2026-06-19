package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 文档版本（pm_doc_version，append-only）。每次保存正文追加一版，支持预览/回滚。
 * content 为 Tiptap JSON；content_text 为纯文本，供摘要与后续全文检索。
 */
@TableName("pm_doc_version")
public class PmDocVersion extends BaseEntity {

    private Long docId;
    private Integer versionNo;
    private String title;
    private String content;
    private String contentText;
    private String changeNote;

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getChangeNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }
}
