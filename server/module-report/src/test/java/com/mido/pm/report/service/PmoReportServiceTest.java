package com.mido.pm.report.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.report.dto.PmoNpssRangeVO;
import com.mido.pm.verify.service.NpssReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PMO 任意周期组织 NPSS（npss-rule §5）：成功%−失败%、对比基线 36、非法区间拒绝。
 */
@ExtendWith(MockitoExtension.class)
class PmoReportServiceTest {

    @Mock private NpssReviewService npssReviewService;
    @Mock private ReportSettingService reportSettingService;
    @InjectMocks private PmoReportService service;

    @Test
    void rangeComputesSuccessMinusFailure() {
        // 60 成功 / 30 混合 / 10 失败 → 60% − 10% = 50，>36 达标
        when(npssReviewService.levelCounts(any(), any()))
                .thenReturn(Map.of("success", 60L, "mixed", 30L, "failure", 10L));
        PmoNpssRangeVO vo = service.pmoNpssRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1));
        assertEquals(100, vo.total());
        assertEquals(0, new BigDecimal("50.00").compareTo(vo.pmoNpss()));
        assertTrue(vo.aboveBaseline());
    }

    @Test
    void rangeRejectsInvalidInterval() {
        assertThrows(BizException.class,
                () -> service.pmoNpssRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void fiscalYearStartMonthShiftsBoundaries() {
        // 财年起始月=4 → 2026 财年 = [2026-04-01, 2027-04-01)
        when(reportSettingService.fiscalYearStartMonth()).thenReturn(4);
        when(npssReviewService.levelCounts(any(), any())).thenReturn(Map.of());

        service.pmoNpss(2026);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(npssReviewService).levelCounts(from.capture(), to.capture());
        assertEquals(LocalDate.of(2026, 4, 1).atStartOfDay(), from.getValue());
        assertEquals(LocalDate.of(2027, 4, 1).atStartOfDay(), to.getValue());
    }
}
