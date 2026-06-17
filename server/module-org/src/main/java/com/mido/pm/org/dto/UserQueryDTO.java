package com.mido.pm.org.dto;

/** 用户复杂查询 DTO（POST /users/query）。page 从 1 起，size 默认 20、上限 100。 */
public record UserQueryDTO(
        Long page,
        Long size,
        String username,
        Long deptId,
        String status) {
}
