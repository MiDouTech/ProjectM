package com.mido.pm.platform.dto;

/** 运营审计分页查询。target/action 可空过滤。 */
public record AuditQueryDTO(String target, String action, Long page, Long size) {
}
