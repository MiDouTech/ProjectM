package com.mido.pm.doc.dto;

/** 文档授权条目。 */
public record DocAclVO(Long id, String principalType, Long principalId, String permission) {
}
