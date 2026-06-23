package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotBlank;

/** RSVP 反馈：accepted 参加 / tentative 暂定 / declined 谢绝。 */
public record RsvpDTO(
        @NotBlank(message = "反馈状态不能为空") String status) {
}
