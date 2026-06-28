package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/**
 * 全局文档列表条目（文档中心首页「全部文档」跨项目扁平列表）。
 * 不含正文；projectId 供前端按项目分组/下钻定位；favorited 为当前用户是否收藏（供收藏筛选）。
 */
public record DocListVO(
        Long id,
        Long projectId,
        String type,
        String title,
        Long updateBy,
        LocalDateTime updateTime,
        boolean favorited) {
}
