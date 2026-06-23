package com.mido.pm.calendar.dto;

import java.time.LocalDateTime;

/** 排期空档（所有参选人皆空闲且时长足够）。 */
public record SlotVO(
        LocalDateTime start,
        LocalDateTime end) {
}
