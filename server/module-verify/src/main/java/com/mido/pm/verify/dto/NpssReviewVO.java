package com.mido.pm.verify.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** NPSS 轮次视图（详情含评分项）。 */
public record NpssReviewVO(
        Long id,
        Long projectId,
        String round,
        String status,
        BigDecimal weightedScore,
        String resultLevel,
        LocalDateTime reviewedAt,
        List<NpssScoreVO> scores) {
}
