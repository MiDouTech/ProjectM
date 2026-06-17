package com.mido.pm.task.dto;

import com.mido.pm.view.dto.ViewConfig;
import jakarta.validation.constraints.NotNull;

/** 按视图查询任务：传 viewId（服务端解析其 config）或直接传 config；projectId 必填。 */
public record ViewQueryRequest(
        @NotNull(message = "项目不能为空") Long projectId,
        Long viewId,
        ViewConfig config) {
}
