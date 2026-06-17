package com.mido.pm.task.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工时记录视图。 */
public record WorkHourVO(
        Long id,
        Long taskId,
        Long userId,
        String kind,
        String category,
        LocalDate workDate,
        BigDecimal hours,
        String remark,
        LocalDateTime createTime) {
}
