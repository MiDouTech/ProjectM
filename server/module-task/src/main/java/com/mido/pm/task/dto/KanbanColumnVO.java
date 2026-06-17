package com.mido.pm.task.dto;

import java.util.List;

/** 看板一列：某状态 + 该状态下任务卡。 */
public record KanbanColumnVO(String status, List<TaskVO> tasks) {
}
