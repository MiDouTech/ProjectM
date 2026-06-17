package com.mido.pm.stakeholder.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 立项默认干系人权重预置（npss-rule §6）。以 6 类角色表达，管理层并入 other（§6 S 模板亦「其他含管理层」）。
 * 注：专项督办受益方=20，低于 §4 的 ≥50%，保留 §6 原样，由 PMO 立项时微调达标（与既定决策一致）。
 */
public final class DefaultWeightTemplate {

    private DefaultWeightTemplate() {
    }

    public static List<RoleWeight> forProject(String category, String subCategory) {
        if ("S".equals(category) || "I".equals(category)) {
            return List.of(rw("sponsor", 30), rw("business", 30), rw("team", 10), rw("finance", 10), rw("other", 20));
        }
        // O
        if ("专项督办".equals(subCategory)) {
            return List.of(rw("regulator", 30), rw("business", 20), rw("team", 10), rw("other", 40));
        }
        // 常规运营 / 定向整改 / 默认 O
        return List.of(rw("business", 50), rw("team", 10), rw("finance", 10), rw("other", 30));
    }

    private static RoleWeight rw(String role, int weight) {
        return new RoleWeight(role, BigDecimal.valueOf(weight));
    }
}
