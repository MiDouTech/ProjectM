package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.QuotaVO;
import com.mido.pm.platform.dto.SubscriptionVO;
import com.mido.pm.platform.dto.TenantCreateDTO;
import com.mido.pm.platform.dto.TenantDetailVO;
import com.mido.pm.platform.dto.TenantQueryDTO;
import com.mido.pm.platform.dto.TenantStatusDTO;
import com.mido.pm.platform.dto.TenantUpdateDTO;
import com.mido.pm.platform.dto.TenantVO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户管理服务：开通/编辑/状态流转/详情。运营手动开通（阶段一不做自助注册）。
 * 新建租户初始为 trial，绑定订阅后转 active（见 {@link PlatformSubscriptionService}）。
 */
@Service
public class TenantAdminService {

    private static final long MAX_PAGE_SIZE = 100L;
    /** 允许的状态流转目标值（运营手动设置） */
    private static final Set<String> SETTABLE_STATUS = Set.of("active", "suspended", "closed");

    private final SysTenantMapper tenantMapper;
    private final PlatformSubscriptionService subscriptionService;
    private final PlatformPlanService planService;
    private final PlatformAuditService auditService;

    public TenantAdminService(SysTenantMapper tenantMapper,
                              PlatformSubscriptionService subscriptionService,
                              PlatformPlanService planService,
                              PlatformAuditService auditService) {
        this.tenantMapper = tenantMapper;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.auditService = auditService;
    }

    public PageResult<TenantVO> page(TenantQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);
        Page<SysTenant> page = new Page<>(pageNo, size);
        Page<SysTenant> result = tenantMapper.selectPage(page, Wrappers.<SysTenant>lambdaQuery()
                .and(StringUtils.hasText(query.keyword()), w -> w
                        .like(SysTenant::getName, query.keyword())
                        .or().like(SysTenant::getCode, query.keyword()))
                .eq(StringUtils.hasText(query.status()), SysTenant::getStatus, query.status())
                .orderByDesc(SysTenant::getId));
        List<TenantVO> list = result.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    public TenantDetailVO detail(Long id) {
        SysTenant t = requireExists(id);
        SubscriptionVO sub = subscriptionService.currentSubscription(id);
        List<QuotaVO> quotas = sub == null ? List.of() : planService.get(sub.planId()).quotas();
        return new TenantDetailVO(t.getId(), t.getCode(), t.getName(), t.getStatus(), t.getIndustry(),
                t.getContactName(), t.getContactPhone(), t.getContactEmail(), t.getSource(), t.getRemark(),
                t.getActivatedAt(), t.getExpireAt(), t.getPurgeScheduledAt(), t.getCreateTime(), sub, quotas);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(TenantCreateDTO dto) {
        Long dup = tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getCode, dto.code()));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "租户编码已存在");
        }
        SysTenant t = new SysTenant();
        t.setCode(dto.code());
        t.setName(dto.name());
        t.setStatus("trial");
        t.setIndustry(dto.industry());
        t.setContactName(dto.contactName());
        t.setContactPhone(dto.contactPhone());
        t.setContactEmail(dto.contactEmail());
        t.setSource("manual");
        t.setRemark(dto.remark());
        tenantMapper.insert(t);
        auditService.record(PlatformAuditActions.TENANT_CREATED, PlatformAuditActions.TARGET_TENANT, t.getId(),
                Map.of("code", dto.code(), "name", dto.name()));
        return t.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TenantUpdateDTO dto) {
        SysTenant t = requireExists(id);
        t.setName(dto.name());
        t.setIndustry(dto.industry());
        t.setContactName(dto.contactName());
        t.setContactPhone(dto.contactPhone());
        t.setContactEmail(dto.contactEmail());
        t.setRemark(dto.remark());
        tenantMapper.updateById(t);
        auditService.record(PlatformAuditActions.TENANT_UPDATED, PlatformAuditActions.TARGET_TENANT, id,
                Map.of("name", dto.name()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, TenantStatusDTO dto) {
        if (!SETTABLE_STATUS.contains(dto.status())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法的目标状态: " + dto.status());
        }
        SysTenant t = requireExists(id);
        String from = t.getStatus();
        t.setStatus(dto.status());
        tenantMapper.updateById(t);
        auditService.record(PlatformAuditActions.TENANT_STATUS_CHANGED, PlatformAuditActions.TARGET_TENANT, id,
                Map.of("from", from, "to", dto.status(), "reason", dto.reason() == null ? "" : dto.reason()));
    }

    private TenantVO toListVO(SysTenant t) {
        SubscriptionVO sub = subscriptionService.currentSubscription(t.getId());
        return new TenantVO(t.getId(), t.getCode(), t.getName(), t.getStatus(), t.getIndustry(),
                t.getContactName(), t.getContactPhone(), sub == null ? null : sub.planName(),
                t.getExpireAt(), t.getCreateTime());
    }

    private SysTenant requireExists(Long id) {
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        return t;
    }
}
