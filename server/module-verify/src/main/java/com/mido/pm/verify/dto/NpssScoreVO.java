package com.mido.pm.verify.dto;

import java.math.BigDecimal;

/** 评分项视图。score 为空表示待打分；subjectId 为所属评价主体（个人口径时为 null），供前端按主体分组展示。 */
public record NpssScoreVO(
        Long id, Long reviewId, Long stakeholderId, Long subjectId, Integer score, BigDecimal weight, String comment) {
}
