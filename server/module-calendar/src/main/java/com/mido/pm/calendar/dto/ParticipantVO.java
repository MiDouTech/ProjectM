package com.mido.pm.calendar.dto;

/** 参与人视图（含 RSVP 状态）。 */
public record ParticipantVO(
        Long id,
        Long userId,
        String externalName,
        String role,
        String rsvpStatus) {
}
