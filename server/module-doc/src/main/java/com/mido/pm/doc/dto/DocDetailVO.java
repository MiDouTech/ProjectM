package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/** 文档详情：节点属性 + 当前版本正文。 */
public record DocDetailVO(
        Long id,
        Long projectId,
        Long parentId,
        String type,
        String title,
        String icon,
        Long currentVersionId,
        Integer versionNo,
        String content,
        Long updateBy,
        LocalDateTime updateTime) {
}
