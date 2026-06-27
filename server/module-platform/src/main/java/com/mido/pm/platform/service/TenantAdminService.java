package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.QuotaVO;
import com.mido.pm.platform.dto.SubscriptionVO;
import com.mido.pm.platform.dto.TenantBatchStatusDTO;
import com.mido.pm.platform.dto.TenantCreateDTO;
import com.mido.pm.platform.dto.TenantDetailVO;
import com.mido.pm.platform.dto.TenantQueryDTO;
import com.mido.pm.platform.dto.TenantStatusDTO;
import com.mido.pm.platform.dto.TenantUpdateDTO;
import com.mido.pm.platform.dto.TenantVO;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户管理服务：开通/编辑/状态流转/详情。运营手动开通（阶段一不做自助注册）。
 * 新建租户初始为 trial，绑定订阅后转 active（见 {@link PlatformSubscriptionService}）。
 */
@Service
public class TenantAdminService {

    private static final Logger log = LoggerFactory.getLogger(TenantAdminService.class);

    private static final long MAX_PAGE_SIZE = 100L;
    /** 允许的状态流转目标值（运营手动设置） */
    private static final Set<String> SETTABLE_STATUS = Set.of("active", "suspended", "closed");

    /** 管理员凭据缺省值（开通时未指定则用此，运营须提示客户首登后修改）。 */
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Mido@2024";

    private final SysTenantMapper tenantMapper;
    private final PlatformSubscriptionService subscriptionService;
    private final PlatformPlanService planService;
    private final PlatformAuditService auditService;
    private final DomainEventPublisher eventPublisher;
    /** 各业务域租户播种器（org/approval/task/project…），Spring 注入全部实现，按 order 升序执行。 */
    private final List<TenantProvisioner> provisioners;
    /** 逐条独立事务模板：批量/到期流转按单租户提交，单条失败不连累其余。 */
    private final TransactionTemplate tx;

    public TenantAdminService(SysTenantMapper tenantMapper,
                              PlatformSubscriptionService subscriptionService,
                              PlatformPlanService planService,
                              PlatformAuditService auditService,
                              DomainEventPublisher eventPublisher,
                              List<TenantProvisioner> provisioners,
                              PlatformTransactionManager txManager) {
        this.tenantMapper = tenantMapper;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.provisioners = provisioners;
        this.tx = new TransactionTemplate(txManager);
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

        String adminUsername = StringUtils.hasText(dto.adminUsername()) ? dto.adminUsername().trim() : DEFAULT_ADMIN_USERNAME;
        String adminPassword = StringUtils.hasText(dto.adminPassword()) ? dto.adminPassword() : DEFAULT_ADMIN_PASSWORD;
        Long adminUserId = provisionTenant(t.getId(), dto.code(), dto.name(), adminUsername, adminPassword);
        if (adminUserId != null) {
            t.setAdminUserId(adminUserId);
            tenantMapper.updateById(t);
        }

        auditService.record(PlatformAuditActions.TENANT_CREATED, PlatformAuditActions.TARGET_TENANT, t.getId(),
                Map.of("code", dto.code(), "name", dto.name(), "adminUsername", adminUsername));
        eventPublisher.publish(PlatformEvents.TENANT_REGISTERED, t.getId(),
                Map.of("tenantId", t.getId(), "code", dto.code(), "name", dto.name()));
        return t.getId();
    }

    /**
     * 租户开通播种：切到新租户 TenantContext，按 order 升序依次调用各域 provisioner 播种默认数据
     * （组织/审批流/状态库/工作项类型/项目类型…），使「开通即可用」。同事务执行，任一失败整体回滚。
     * 返回组织域生成的管理员用户 id（供回填 sys_tenant.admin_user_id），无则 null。
     */
    private Long provisionTenant(Long tenantId, String code, String name, String adminUsername, String adminPassword) {
        TenantProvisionContext ctx = new TenantProvisionContext(tenantId, code, name, adminUsername, adminPassword);
        Long prev = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            provisioners.stream().sorted(Comparator.comparingInt(TenantProvisioner::order))
                    .forEach(p -> p.provision(ctx));
        } finally {
            if (prev != null) {
                TenantContext.set(prev);
            } else {
                TenantContext.clear();
            }
        }
        return ctx.get(TenantProvisionContext.KEY_ADMIN_USER_ID);
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
        doChangeStatus(id, dto);
    }

    /**
     * 批量状态流转：每个租户独立事务提交，单条失败仅跳过该条、不回滚其余（避免一条非法状态/并发冲突
     * 拖垮整批）。返回成功处理数量。
     */
    public int batchChangeStatus(TenantBatchStatusDTO dto) {
        int ok = 0;
        for (Long id : dto.ids()) {
            try {
                tx.executeWithoutResult(s -> doChangeStatus(id, new TenantStatusDTO(dto.status(), dto.reason())));
                ok++;
            } catch (RuntimeException e) {
                log.warn("批量状态流转单条失败 tenantId={} target={}", id, dto.status(), e);
            }
        }
        return ok;
    }

    private void doChangeStatus(Long id, TenantStatusDTO dto) {
        if (!SETTABLE_STATUS.contains(dto.status())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法的目标状态: " + dto.status());
        }
        SysTenant t = requireExists(id);
        String from = t.getStatus();
        t.setStatus(dto.status());
        tenantMapper.updateById(t);
        auditService.record(PlatformAuditActions.TENANT_STATUS_CHANGED, PlatformAuditActions.TARGET_TENANT, id,
                Map.of("from", from, "to", dto.status(), "reason", dto.reason() == null ? "" : dto.reason()));
        eventPublisher.publish(PlatformEvents.TENANT_STATUS_CHANGED, id,
                Map.of("tenantId", id, "from", from, "to", dto.status()));
    }

    /**
     * 到期自动流转：把 trial/active 且已过 expire_at 的租户置为 expired（系统动作）。
     * 自用租户(expire_at 为空)天然不在范围。每个租户独立事务提交，单条失败不影响其余。
     * 返回成功流转数量。供 {@link PlatformMaintenanceScheduler} 每日调用。
     */
    public int expireOverdue() {
        List<SysTenant> due = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .in(SysTenant::getStatus, "trial", "active")
                .isNotNull(SysTenant::getExpireAt)
                .lt(SysTenant::getExpireAt, java.time.LocalDateTime.now()));
        int ok = 0;
        for (SysTenant t : due) {
            try {
                tx.executeWithoutResult(s -> doExpire(t));
                ok++;
            } catch (RuntimeException e) {
                log.warn("到期流转单条失败 tenantId={}", t.getId(), e);
            }
        }
        return ok;
    }

    private void doExpire(SysTenant t) {
        String from = t.getStatus();
        t.setStatus("expired");
        tenantMapper.updateById(t);
        auditService.record(PlatformAuditActions.TENANT_EXPIRED, PlatformAuditActions.TARGET_TENANT, t.getId(),
                Map.of("from", from, "to", "expired", "expireAt", String.valueOf(t.getExpireAt())));
        eventPublisher.publish(PlatformEvents.TENANT_EXPIRED, t.getId(),
                Map.of("tenantId", t.getId(), "expireAt", String.valueOf(t.getExpireAt())));
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
