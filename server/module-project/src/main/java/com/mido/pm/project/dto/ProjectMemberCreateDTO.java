package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotNull;

public record ProjectMemberCreateDTO(
        @NotNull(message = "成员用户不能为空") Long userId,
        String projectRole) {
}
