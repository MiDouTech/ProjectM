package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.platform.dto.DashboardVO;
import com.mido.pm.platform.dto.TenantVO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营概览：租户规模、状态分布、本月新增、近 30 天到期预警。
 */
@Service
public class PlatformDashboardService {

    private static final int EXPIRING_WINDOW_DAYS = 30;
    private static final int EXPIRING_LIMIT = 20;

    private final SysTenantMapper tenantMapper;

    public PlatformDashboardService(SysTenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    public DashboardVO overview() {
        long total = count(null);
        long active = count("active");
        long trial = count("trial");

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery()
                .ge(SysTenant::getCreateTime, monthStart));

        Map<String, Long> dist = new LinkedHashMap<>();
        for (String s : List.of("trial", "active", "suspended", "expired", "closed")) {
            dist.put(s, count(s));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(EXPIRING_WINDOW_DAYS);
        List<TenantVO> expiring = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                        .isNotNull(SysTenant::getExpireAt)
                        .ge(SysTenant::getExpireAt, now)
                        .le(SysTenant::getExpireAt, soon)
                        .eq(SysTenant::getStatus, "active")
                        .orderByAsc(SysTenant::getExpireAt)
                        .last("limit " + EXPIRING_LIMIT))
                .stream()
                .map(t -> new TenantVO(t.getId(), t.getCode(), t.getName(), t.getStatus(), t.getIndustry(),
                        t.getContactName(), t.getContactPhone(), null, t.getExpireAt(), t.getCreateTime()))
                .toList();

        return new DashboardVO(total, active, trial, newThisMonth, dist, expiring);
    }

    private long count(String status) {
        return tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery()
                .eq(status != null, SysTenant::getStatus, status));
    }
}
