package com.mido.pm.platform.service;

import com.mido.pm.platform.dto.RevenueSummaryVO;
import com.mido.pm.platform.entity.SysRevenueRecord;
import com.mido.pm.platform.mapper.SysRevenueRecordMapper;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 收入汇总单测：收款/退款分类与净额。 */
@ExtendWith(MockitoExtension.class)
class PlatformRevenueServiceTest {

    @Mock
    private SysRevenueRecordMapper revenueMapper;
    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private PlatformAuditService auditService;
    @InjectMocks
    private PlatformRevenueService service;

    private SysRevenueRecord rec(String type, String amount) {
        SysRevenueRecord r = new SysRevenueRecord();
        r.setType(type);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    @Test
    void summaryComputesNet() {
        when(revenueMapper.selectList(any())).thenReturn(List.of(
                rec("payment", "1000.00"),
                rec("payment", "500.00"),
                rec("refund", "200.00")));
        RevenueSummaryVO s = service.summary(null);
        assertEquals(new BigDecimal("1500.00"), s.totalPayment());
        assertEquals(new BigDecimal("200.00"), s.totalRefund());
        assertEquals(new BigDecimal("1300.00"), s.net());
        assertEquals(3, s.count());
    }
}
