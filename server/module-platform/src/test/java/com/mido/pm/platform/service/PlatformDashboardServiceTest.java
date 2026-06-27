package com.mido.pm.platform.service;

import com.mido.pm.platform.dto.TrendPointVO;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 运营概览趋势单测：补齐近12月、缺失月补0。 */
@ExtendWith(MockitoExtension.class)
class PlatformDashboardServiceTest {

    @Mock
    private SysTenantMapper tenantMapper;
    @InjectMocks
    private PlatformDashboardService service;

    @Test
    void tenantTrendReturns12MonthsWithZeroFill() {
        String thisMonth = String.format("%04d-%02d", LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        when(tenantMapper.monthlyRegistrations(any())).thenReturn(List.of(Map.of("ym", thisMonth, "cnt", 7)));

        List<TrendPointVO> trend = service.tenantTrend();

        assertEquals(12, trend.size());
        assertEquals(thisMonth, trend.get(11).month());
        assertEquals(7L, trend.get(11).value());
        assertEquals(0L, trend.get(0).value());
    }
}
