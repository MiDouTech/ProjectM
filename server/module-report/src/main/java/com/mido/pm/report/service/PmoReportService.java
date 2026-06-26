package com.mido.pm.report.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.report.domain.PmoNpssCalculator;
import com.mido.pm.report.dto.PmoNpssRangeVO;
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
    private final ReportSettingService reportSettingService;

    public PmoReportService(NpssReviewService npssReviewService, ReportSettingService reportSettingService) {
        this.npssReviewService = npssReviewService;
        this.reportSettingService = reportSettingService;
    }

    /**
     * PMO 财年口径（npss-rule §5）：财年 year 起于配置的起始月。
     * 起始月=1 即自然年 [year-01-01, year+1-01-01)；起始月=M 即 [year-M-01, year+1-M-01)。
     */
    public PmoNpssVO pmoNpss(int year) {
        int startMonth = reportSettingService.fiscalYearStartMonth();
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = start.plusYears(1).atStartOfDay();
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

    /**
     * 任意周期组织 NPSS（npss-rule §5）：统计 [from, to)（按 reviewed_at）内已汇总轮次的成功%−失败%。
     * 动态计算组织在一定周期内的 NPSS；to 须晚于 from。
     */
    public PmoNpssRangeVO pmoNpssRange(LocalDate from, LocalDate toExclusive) {
        if (from == null || toExclusive == null || !toExclusive.isAfter(from)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "统计区间非法：to 须晚于 from");
        }
        Map<String, Long> counts = npssReviewService.levelCounts(from.atStartOfDay(), toExclusive.atStartOfDay());
        long success = counts.getOrDefault(ResultLevel.SUCCESS.getCode(), 0L);
        long mixed = counts.getOrDefault(ResultLevel.MIXED.getCode(), 0L);
        long failure = counts.getOrDefault(ResultLevel.FAILURE.getCode(), 0L);

        PmoNpssCalculator.Stats s = PmoNpssCalculator.compute(success, mixed, failure);
        return new PmoNpssRangeVO(from, toExclusive, s.total(), success, mixed, failure,
                s.successRate(), s.failureRate(), s.pmoNpss(),
                PmoNpssCalculator.BASELINE,
                s.pmoNpss().compareTo(PmoNpssCalculator.BASELINE) > 0);
    }
}
