package com.mido.pm.doc.dto;

/** 保存文档正文：生成新版本。content 为 Tiptap JSON，contentText 为纯文本（可选，前端抽取）。 */
public record DocSaveDTO(
        String title,
        String content,
        String contentText,
        String changeNote) {
}
