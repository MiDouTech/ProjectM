package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.PlanSaveDTO;
import com.mido.pm.platform.dto.PlanVO;
import com.mido.pm.platform.dto.QuotaDTO;
import com.mido.pm.platform.dto.QuotaVO;
import com.mido.pm.platform.entity.SysPlan;
import com.mido.pm.platform.entity.SysPlanQuota;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanMapper;
import com.mido.pm.platform.mapper.SysPlanQuotaMapper;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 套餐与配额服务：套餐 CRUD + 配额项整体覆盖。
 * 配额项是套餐的从属集合，保存时先清后插（整体覆盖语义）。
 */
@Service
public class PlatformPlanService {

    private final SysPlanMapper planMapper;
    private final SysPlanQuotaMapper quotaMapper;
    private final SysTenantSubscriptionMapper subscriptionMapper;
    private final PlatformAuditService auditService;

    public PlatformPlanService(SysPlanMapper planMapper, SysPlanQuotaMapper quotaMapper,
                               SysTenantSubscriptionMapper subscriptionMapper,
                               PlatformAuditService auditService) {
        this.planMapper = planMapper;
        this.quotaMapper = quotaMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.auditService = auditService;
    }

    public List<PlanVO> list() {
        List<SysPlan> plans = planMapper.selectList(Wrappers.<SysPlan>lambdaQuery()
                .orderByAsc(SysPlan::getSort).orderByAsc(SysPlan::getId));
        return plans.stream().map(p -> toVO(p, quotasOf(p.getId()))).toList();
    }

    public PlanVO get(Long id) {
        SysPlan plan = requireExists(id);
        return toVO(plan, quotasOf(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PlanSaveDTO dto) {
        Long dup = planMapper.selectCount(Wrappers.<SysPlan>lambdaQuery().eq(SysPlan::getCode, dto.code()));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "套餐编码已存在");
        }
        SysPlan plan = new SysPlan();
        plan.setCode(dto.code());
        applyBasic(plan, dto);
        planMapper.insert(plan);
        saveQuotas(plan.getId(), dto.quotas());
        auditService.record(PlatformAuditActions.PLAN_SAVED, PlatformAuditActions.TARGET_PLAN, plan.getId(),
                Map.of("op", "create", "code", dto.code(), "name", dto.name()));
        return plan.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PlanSaveDTO dto) {
        SysPlan plan = requireExists(id);
        applyBasic(plan, dto);
        planMapper.updateById(plan);
        saveQuotas(id, dto.quotas());
        auditService.record(PlatformAuditActions.PLAN_SAVED, PlatformAuditActions.TARGET_PLAN, id,
                Map.of("op", "update", "name", dto.name()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        Long inUse = subscriptionMapper.selectCount(Wrappers.<SysTenantSubscription>lambdaQuery()
                .eq(SysTenantSubscription::getPlanId, id)
                .eq(SysTenantSubscription::getStatus, "active"));
        if (inUse != null && inUse > 0) {
            throw new BizException(ErrorCode.CONFLICT, "套餐已被租户订阅，不能删除");
        }
        planMapper.deleteById(id);
        quotaMapper.delete(Wrappers.<SysPlanQuota>lambdaQuery().eq(SysPlanQuota::getPlanId, id));
        auditService.record(PlatformAuditActions.PLAN_DELETED, PlatformAuditActions.TARGET_PLAN, id, null);
    }

    /** 某套餐的配额项（resource → limitValue）。 */
    public Map<String, Long> quotaMapOf(Long planId) {
        return quotasOf(planId).stream().collect(Collectors.toMap(QuotaVO::resource, QuotaVO::limitValue));
    }

    private void applyBasic(SysPlan plan, PlanSaveDTO dto) {
        plan.setName(dto.name());
        plan.setPrice(dto.price());
        plan.setBillingCycle(StringUtils.hasText(dto.billingCycle()) ? dto.billingCycle() : "yearly");
        plan.setStatus(StringUtils.hasText(dto.status()) ? dto.status() : "active");
        plan.setSort(dto.sort() == null ? 0 : dto.sort());
        plan.setRemark(dto.remark());
    }

    private void saveQuotas(Long planId, List<QuotaDTO> quotas) {
        quotaMapper.delete(Wrappers.<SysPlanQuota>lambdaQuery().eq(SysPlanQuota::getPlanId, planId));
        if (quotas == null) {
            return;
        }
        for (QuotaDTO q : quotas) {
            SysPlanQuota entity = new SysPlanQuota();
            entity.setPlanId(planId);
            entity.setResource(q.resource());
            entity.setLimitValue(q.limitValue());
            quotaMapper.insert(entity);
        }
    }

    private List<QuotaVO> quotasOf(Long planId) {
        return quotaMapper.selectList(Wrappers.<SysPlanQuota>lambdaQuery()
                        .eq(SysPlanQuota::getPlanId, planId).orderByAsc(SysPlanQuota::getId))
                .stream().map(q -> new QuotaVO(q.getResource(), q.getLimitValue())).toList();
    }

    private SysPlan requireExists(Long id) {
        SysPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        return plan;
    }

    private PlanVO toVO(SysPlan p, List<QuotaVO> quotas) {
        return new PlanVO(p.getId(), p.getCode(), p.getName(), p.getPrice(), p.getBillingCycle(),
                p.getStatus(), p.getSort(), p.getRemark(), quotas);
    }
}
