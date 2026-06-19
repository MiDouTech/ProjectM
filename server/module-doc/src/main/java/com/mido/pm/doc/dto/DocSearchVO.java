package com.mido.pm.doc.dto;

/** 文档搜索结果（命中标题或正文）。snippet 为正文片段，命中标题时可为空。 */
public record DocSearchVO(Long id, String type, String title, String snippet) {
}
