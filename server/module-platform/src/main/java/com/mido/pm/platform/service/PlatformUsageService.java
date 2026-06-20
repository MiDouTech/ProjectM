package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.platform.dto.TenantUsageVO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.entity.SysTenantQuotaUsage;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.mapper.SysTenantQuotaUsageMapper;
import com.mido.pm.platform.support.PlatformTenantScope;
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

    private final SysTenantMapper tenantMapper;
    private final SysTenantQuotaUsageMapper usageMapper;
    private final PlatformQuotaService quotaService;
    private final List<UsageContributor> contributors;

    public PlatformUsageService(SysTenantMapper tenantMapper, SysTenantQuotaUsageMapper usageMapper,
                                PlatformQuotaService quotaService, List<UsageContributor> contributors) {
        this.tenantMapper = tenantMapper;
        this.usageMapper = usageMapper;
        this.quotaService = quotaService;
        this.contributors = contributors;
    }

    /** 对全部未注销租户做一次用量快照。返回处理的租户数。 */
    public int snapshotAll() {
        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .ne(SysTenant::getStatus, "closed"));
        for (SysTenant t : tenants) {
            snapshotTenant(t.getId());
        }
        return tenants.size();
    }

    /** 对单个租户做用量快照（按租户上下文统计后 upsert）。 */
    @Transactional(rollbackFor = Exception.class)
    public void snapshotTenant(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        try (PlatformTenantScope ignored = PlatformTenantScope.of(tenantId)) {
            for (UsageContributor c : contributors) {
                upsert(tenantId, c.resource(), c.currentCount(), now);
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

    private void upsert(Long tenantId, String resource, long used, LocalDateTime now) {
        SysTenantQuotaUsage existing = usageMapper.selectOne(Wrappers.<SysTenantQuotaUsage>lambdaQuery()
                .eq(SysTenantQuotaUsage::getTenantId, tenantId)
                .eq(SysTenantQuotaUsage::getResource, resource)
                .last("limit 1"));
        if (existing == null) {
            SysTenantQuotaUsage row = new SysTenantQuotaUsage();
            row.setTenantId(tenantId);
            row.setResource(resource);
            row.setUsedValue(used);
            row.setSnapshotTime(now);
            usageMapper.insert(row);
        } else {
            existing.setUsedValue(used);
            existing.setSnapshotTime(now);
            usageMapper.updateById(existing);
        }
    }
}
