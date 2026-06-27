package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.platform.dto.TenantUsageOverviewVO;
import com.mido.pm.platform.dto.TenantUsageVO;
import com.mido.pm.platform.dto.UsageMonitorQueryDTO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.entity.SysTenantQuotaUsage;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.mapper.SysTenantQuotaUsageMapper;
import com.mido.pm.platform.support.PlatformTenantScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户用量统计：按租户切上下文聚合各业务域 {@link UsageContributor} 的实时数量，upsert 快照；
 * 并按生效配额计算用量视图（含是否超限）。
 */
@Service
public class PlatformUsageService {

    private static final Logger log = LoggerFactory.getLogger(PlatformUsageService.class);

    private final SysTenantMapper tenantMapper;
    private final SysTenantQuotaUsageMapper usageMapper;
    private final PlatformQuotaService quotaService;
    private final List<UsageContributor> contributors;
    /** 自身代理：经它调用 snapshotTenant 才能让 @Transactional 生效（避免 this 自调用绕过 AOP）。 */
    private final PlatformUsageService self;

    public PlatformUsageService(SysTenantMapper tenantMapper, SysTenantQuotaUsageMapper usageMapper,
                                PlatformQuotaService quotaService, List<UsageContributor> contributors,
                                @Lazy PlatformUsageService self) {
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
        this.quotaService = quotaService;
        this.contributors = contributors;
        this.self = self;
    }

    /** 对全部未注销租户做一次用量快照。逐租户独立事务，单租户失败不影响其余。返回成功数。 */
    public int snapshotAll() {
        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .ne(SysTenant::getStatus, "closed"));
        int ok = 0;
        for (SysTenant t : tenants) {
            try {
                self.snapshotTenant(t.getId());
                ok++;
            } catch (Exception e) {
                log.error("租户用量快照失败 tenantId={}", t.getId(), e);
            }
        }
        return ok;
    }

    /** 对单个租户做用量快照（按租户上下文统计后原子 upsert）。 */
    @Transactional(rollbackFor = Exception.class)
    public void snapshotTenant(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        try (PlatformTenantScope ignored = PlatformTenantScope.of(tenantId)) {
            for (UsageContributor c : contributors) {
                SysTenantQuotaUsage row = new SysTenantQuotaUsage();
                row.setId(IdWorker.getId());
                row.setTenantId(tenantId);
                row.setResource(c.resource());
                row.setUsedValue(c.currentCount());
                row.setSnapshotTime(now);
                usageMapper.upsert(row);
            }
        }
    }

    /** 租户各资源用量视图（含上限与是否超限）；以最近快照为准。 */
    public List<TenantUsageVO> usageOf(Long tenantId) {
        Map<String, Long> limits = quotaService.effectiveLimits(tenantId);
        List<SysTenantQuotaUsage> rows = usageMapper.selectList(Wrappers.<SysTenantQuotaUsage>lambdaQuery()
                .eq(SysTenantQuotaUsage::getTenantId, tenantId));
        List<TenantUsageVO> result = new ArrayList<>();
        for (String resource : QuotaResources.ALL) {
            SysTenantQuotaUsage row = rows.stream()
                    .filter(r -> resource.equals(r.getResource())).findFirst().orElse(null);
            long used = row == null || row.getUsedValue() == null ? 0L : row.getUsedValue();
            long limit = limits.getOrDefault(resource, PlatformQuotaService.UNLIMITED);
            boolean exceeded = limit >= 0 && used > limit;
            result.add(new TenantUsageVO(resource, used, limit, exceeded,
                    row == null ? null : row.getSnapshotTime()));
        }
        return result;
    }

    /**
     * 跨租户用量监控：列出未注销租户的用量/配额概览，可仅看超限租户。
     * 阶段一租户规模小，按全量计算后内存分页；规模化后再下沉聚合查询。
     */
    public PageResult<TenantUsageOverviewVO> pageTenantUsage(UsageMonitorQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), 100L);
        boolean onlyExceeded = Boolean.TRUE.equals(query.onlyExceeded());

        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .ne(SysTenant::getStatus, "closed")
                .orderByDesc(SysTenant::getId));
        List<TenantUsageOverviewVO> all = new ArrayList<>();
        for (SysTenant t : tenants) {
            List<TenantUsageVO> usage = usageOf(t.getId());
            boolean anyExceeded = usage.stream().anyMatch(TenantUsageVO::exceeded);
            if (onlyExceeded && !anyExceeded) {
                continue;
            }
            all.add(new TenantUsageOverviewVO(t.getId(), t.getCode(), t.getName(), t.getStatus(), usage, anyExceeded));
        }
        long total = all.size();
        int from = (int) Math.min((pageNo - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        return PageResult.of(all.subList(from, to), total, pageNo, size);
    }

    /** 当前快照下已超出生效配额上限的资源列表（供降级时存量超额检测）。 */
    public List<String> overQuotaResources(Long tenantId) {
        List<String> over = new ArrayList<>();
        for (TenantUsageVO vo : usageOf(tenantId)) {
            if (vo.exceeded()) {
                over.add(vo.resource());
            }
        }
        return over;
    }
}
