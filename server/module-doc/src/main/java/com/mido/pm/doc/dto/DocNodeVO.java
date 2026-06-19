package com.mido.pm.doc.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 文档目录树节点（不含正文）。children 递归构成树。 */
public record DocNodeVO(
        Long id,
        Long parentId,
        String type,
        String title,
        String icon,
        Integer sortNo,
        Long updateBy,
        LocalDateTime updateTime,
        List<DocNodeVO> children) {
}
