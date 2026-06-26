package com.mido.pm.task.dto;

/**
 * 任务关联条目（追溯链）。direction=outgoing(本任务为源)/incoming(本任务为目标)；
 * relatedTask* 为关联的另一端任务信息。
 */
public record TaskRelationVO(
        Long id,
        String relationKind,
        String direction,
        Long relatedTaskId,
        String relatedTitle,
        String relatedStatus) {
}
