package com.mido.pm.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 授权：principalType=user/role，permission=read/write/admin。 */
public record DocAclGrantDTO(
        @NotBlank String principalType,
        @NotNull Long principalId,
        @NotBlank String permission) {
}
