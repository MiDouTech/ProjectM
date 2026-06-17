package com.mido.pm.verify.dto;

import java.math.BigDecimal;

/** 评分项视图。score 为空表示待打分。 */
public record NpssScoreVO(
        Long id, Long reviewId, Long stakeholderId, Integer score, BigDecimal weight, String comment) {
}
