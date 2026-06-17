package com.mido.pm.org.dto;

import java.time.LocalDateTime;

/** 用户视图（不含 password）。 */
public record UserVO(
        Long id,
        String username,
        String name,
        Long deptId,
        String jobLevel,
        String status,
        LocalDateTime createTime) {
}
