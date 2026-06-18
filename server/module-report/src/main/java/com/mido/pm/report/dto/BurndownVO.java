package com.mido.pm.report.dto;

import java.util.List;

/** 燃尽图数据：按 due_date 的计划剩余（total − 截至该日应完成累计）。阶段一为计划线；实际燃尽需 completed_at（V1 无）。 */
public record BurndownVO(long total, List<BurndownPoint> points) {

    public record BurndownPoint(String date, long remaining) {
    }
}
