package com.mido.pm.briefing.dto;

import java.time.LocalDate;

/** 跟进问题视图。 */
public record IssueVO(
        Long id,
        Long briefingId,
        Long raisedBy,
        Long ownerId,
        String content,
        String status,
        LocalDate dueDate) {
}
