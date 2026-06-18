package com.mido.pm.org.dto;

import java.time.LocalDateTime;

/** 用户视图（不含 password）。 */
public record UserVO(
        Long id,
        String phone,
        String username,
        String name,
        String avatar,
        Long deptId,
        String jobLevel,
        String status,
        LocalDateTime createTime) {
}
