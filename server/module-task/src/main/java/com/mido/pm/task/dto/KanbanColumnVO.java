package com.mido.pm.task.dto;

import java.util.List;

/**
 * 看板一列：某状态 + 颜色(设计 token) + 元类别 + 该状态下任务卡。
 * color/metaCategory 来自状态库（未配置状态库时为 null，前端按状态名回落着色）。
 */
public record KanbanColumnVO(String status, String color, String metaCategory, List<TaskVO> tasks) {
}
