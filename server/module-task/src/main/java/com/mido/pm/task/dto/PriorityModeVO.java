package com.mido.pm.task.dto;

import java.util.List;

/** 优先级模式对外视图（含档位）。 */
public record PriorityModeVO(
        Long id, String name, String remark, Integer builtin, String status,
        List<PriorityLevelDTO> levels) {
}
