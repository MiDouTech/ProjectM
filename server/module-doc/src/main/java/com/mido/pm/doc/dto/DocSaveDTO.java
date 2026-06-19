package com.mido.pm.doc.dto;

/**
 * 保存文档正文：生成新版本。content 为 Tiptap JSON，contentText 为纯文本（可选，前端抽取）。
 * baseVersionId：客户端载入时的当前版本 id，用于乐观并发校验（轻量防冲突）；新文档传 null。
 */
public record DocSaveDTO(
        String title,
        String content,
        String contentText,
        String changeNote,
        Long baseVersionId) {
}
