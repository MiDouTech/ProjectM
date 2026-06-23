package com.mido.pm.calendar.dto;

/**
 * 参与人入参：内部成员填 userId，外部参与人填 externalName；role 缺省 required。
 */
public record ParticipantInputDTO(
        Long userId,
        String externalName,
        String role) {
}
