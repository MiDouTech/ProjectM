package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.SubscriptionSaveDTO;
import com.mido.pm.platform.dto.SubscriptionVO;
import com.mido.pm.platform.entity.SysPlan;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanMapper;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户订阅服务：绑定/续期套餐，并同步租户的到期时间与状态。
 * 不变量：每个租户至多一条 active 订阅；绑定新订阅时把旧 active 置为 cancelled。
 */
@Service
public class PlatformSubscriptionService {

    private static final String ACTIVE = "active";

    private final SysTenantSubscriptionMapper subscriptionMapper;
    private final SysTenantMapper tenantMapper;
    private final SysPlanMapper planMapper;
    private final PlatformAuditService auditService;

    public PlatformSubscriptionService(SysTenantSubscriptionMapper subscriptionMapper,
                                       SysTenantMapper tenantMapper, SysPlanMapper planMapper,
                                       PlatformAuditService auditService) {
        this.subscriptionMapper = subscriptionMapper;
        this.tenantMapper = tenantMapper;
        this.planMapper = planMapper;
        this.auditService = auditService;
    }

    /** 绑定/续期租户订阅，并同步租户 expireAt 与状态。 */
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long tenantId, SubscriptionSaveDTO dto) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        SysPlan plan = planMapper.selectById(dto.planId());
        if (plan == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        // 旧 active 订阅置为 cancelled（保留历史，不物理删）
        SysTenantSubscription mark = new SysTenantSubscription();
        mark.setStatus("cancelled");
        subscriptionMapper.update(mark, Wrappers.<SysTenantSubscription>lambdaUpdate()
                .eq(SysTenantSubscription::getTenantId, tenantId)
                .eq(SysTenantSubscription::getStatus, ACTIVE));

        SysTenantSubscription sub = new SysTenantSubscription();
        sub.setTenantId(tenantId);
        sub.setPlanId(dto.planId());
        sub.setStartAt(dto.startAt() == null ? LocalDateTime.now() : dto.startAt());
        sub.setExpireAt(dto.expireAt());
        sub.setStatus(ACTIVE);
        sub.setRemark(dto.remark());
        subscriptionMapper.insert(sub);

        // 同步租户：到期时间随订阅，状态恢复为 active，首次绑定记激活时间
        tenant.setExpireAt(dto.expireAt());
        tenant.setStatus("active");
        if (tenant.getActivatedAt() == null) {
            tenant.setActivatedAt(LocalDateTime.now());
        }
        tenantMapper.updateById(tenant);

        Map<String, Object> detail = new HashMap<>();
        detail.put("planId", dto.planId());
        detail.put("planName", plan.getName());
        detail.put("expireAt", dto.expireAt());
        auditService.record(PlatformAuditActions.SUBSCRIPTION_SAVED,
                PlatformAuditActions.TARGET_SUBSCRIPTION, sub.getId(), detail);
    }

    /** 租户当前生效订阅（无则 null）。 */
    public SubscriptionVO currentSubscription(Long tenantId) {
        SysTenantSubscription sub = subscriptionMapper.selectOne(Wrappers.<SysTenantSubscription>lambdaQuery()
                .eq(SysTenantSubscription::getTenantId, tenantId)
                .eq(SysTenantSubscription::getStatus, ACTIVE)
                .orderByDesc(SysTenantSubscription::getId)
                .last("limit 1"));
        if (sub == null) {
            return null;
        }
        SysPlan plan = planMapper.selectById(sub.getPlanId());
        return new SubscriptionVO(sub.getId(), sub.getPlanId(), plan == null ? null : plan.getName(),
                sub.getStatus(), sub.getStartAt(), sub.getExpireAt(), sub.getRemark());
    }
}
