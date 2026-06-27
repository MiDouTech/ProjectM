package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.platform.dto.RevenueRecordDTO;
import com.mido.pm.platform.dto.RevenueSummaryVO;
import com.mido.pm.platform.entity.SysRevenueRecord;
import com.mido.pm.platform.entity.SysTenant;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void refundExceedingCollectedRejected() {
        when(tenantMapper.selectById(1L)).thenReturn(new SysTenant());
        // 已收净额 = 1000 - 0 = 1000，退款 1500 应被拒
        when(revenueMapper.selectList(any())).thenReturn(List.of(rec("payment", "1000.00")));
        RevenueRecordDTO dto = new RevenueRecordDTO(1L, "refund", new BigDecimal("1500.00"), null, null, null);
        assertThrows(BizException.class, () -> service.create(dto));
    }

    @Test
    void refundWithinCollectedAllowed() {
        when(tenantMapper.selectById(1L)).thenReturn(new SysTenant());
        when(revenueMapper.selectList(any())).thenReturn(List.of(rec("payment", "1000.00")));
        RevenueRecordDTO dto = new RevenueRecordDTO(1L, "refund", new BigDecimal("300.00"), null, null, null);
        // 不抛异常即通过（insert 由 mock 吞掉）
        service.create(dto);
    }
}
