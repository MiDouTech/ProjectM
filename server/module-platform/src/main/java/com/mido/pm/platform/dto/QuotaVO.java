package com.mido.pm.platform.dto;

/** 配额项视图。limitValue=-1 表示不限。 */
public record QuotaVO(String resource, Long limitValue) {
}
