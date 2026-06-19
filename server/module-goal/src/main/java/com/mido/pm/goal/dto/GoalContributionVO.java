package com.mido.pm.goal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * KR 贡献度视图（反向看板）：某 KR 由哪些对齐项目支撑、各自完成率/权重/贡献占比。
 * weightedRate = Σ(完成率×权重)/Σ权重；item.contribution = 完成率×权重/Σ权重（Σ各项=weightedRate）。
 * 项目名称由前端按 projectId 解析（目标域不耦合 project）。
 */
public record GoalContributionVO(
        Long goalId,
        String title,
        BigDecimal weightedRate,
        List<Item> items) {

    public record Item(
            Long projectId,
            BigDecimal weight,
            BigDecimal completionRate,
            BigDecimal contribution) {
    }
}
