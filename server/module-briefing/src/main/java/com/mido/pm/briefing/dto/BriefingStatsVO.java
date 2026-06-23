package com.mido.pm.briefing.dto;

import java.util.List;

/** 简报统计：我评审范围内某类型的提交概览。 */
public record BriefingStatsVO(
        String type,
        long total,
        List<MemberStat> members) {

    /** 单成员提交统计。 */
    public record MemberStat(Long authorId, long submittedCount) {
    }
}
