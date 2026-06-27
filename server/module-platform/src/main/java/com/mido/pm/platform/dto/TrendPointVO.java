package com.mido.pm.platform.dto;

/** 趋势数据点（month=YYYY-MM，value=数值）。 */
public record TrendPointVO(String month, long value) {
}
