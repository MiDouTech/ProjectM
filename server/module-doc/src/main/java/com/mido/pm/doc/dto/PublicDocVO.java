package com.mido.pm.doc.dto;

/** 公开分享的只读文档（匿名访问，不含敏感字段）。 */
public record PublicDocVO(String title, String content) {
}
