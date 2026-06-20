package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/** 租户数据导出任务视图。fileReady=可下载。 */
public record ExportTaskVO(
        Long id,
        Long tenantId,
        String status,
        boolean fileReady,
        String error,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
