package com.mido.pm.report.service;

import com.mido.pm.report.domain.PmoNpssCalculator;
import com.mido.pm.report.dto.PmoNpssVO;
import com.mido.pm.verify.domain.ResultLevel;
import com.mido.pm.verify.service.NpssReviewService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PMO 报表：按财年聚合 NPSS result_level → 成功%−失败%（npss-rule §5）。
 * 财年口径：自然年（year-01-01 ~ 次年-01-01，按 reviewed_at）；如需公司财年起月可后续配置。
 * result_level 数据归属验收域，经 {@link NpssReviewService} 取计数，不跨域查表。
 */
@Service
public class PmoReportService {

    private final NpssReviewService npssReviewService;

    public PmoReportService(NpssReviewService npssReviewService) {
        this.npssReviewService = npssReviewService;
    }

    public PmoNpssVO pmoNpss(int year) {
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime toExclusive = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        Map<String, Long> counts = npssReviewService.levelCounts(from, toExclusive);
        long success = counts.getOrDefault(ResultLevel.SUCCESS.getCode(), 0L);
        long mixed = counts.getOrDefault(ResultLevel.MIXED.getCode(), 0L);
        long failure = counts.getOrDefault(ResultLevel.FAILURE.getCode(), 0L);

        PmoNpssCalculator.Stats s = PmoNpssCalculator.compute(success, mixed, failure);
        return new PmoNpssVO(year, s.total(), success, mixed, failure,
                s.successRate(), s.failureRate(), s.pmoNpss(),
                PmoNpssCalculator.BASELINE,
                s.pmoNpss().compareTo(PmoNpssCalculator.BASELINE) > 0);
    }
}
