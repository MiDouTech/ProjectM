package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.RevenueQueryDTO;
import com.mido.pm.platform.dto.RevenueRecordDTO;
import com.mido.pm.platform.dto.RevenueSummaryVO;
import com.mido.pm.platform.dto.RevenueVO;
import com.mido.pm.platform.dto.SubscriptionVO;
import com.mido.pm.platform.entity.SysRevenueRecord;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysRevenueRecordMapper;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 线下收入台账：流水 CRUD + 汇总。type=payment 收款 / refund 退款。 */
@Service
public class PlatformRevenueService {

    private static final long MAX_PAGE_SIZE = 100L;
    private static final String TYPE_PAYMENT = "payment";
    private static final String TYPE_REFUND = "refund";

    private static final String DEFAULT_CURRENCY = "CNY";

    private final SysRevenueRecordMapper revenueMapper;
    private final SysTenantMapper tenantMapper;
    private final PlatformAuditService auditService;
    private final PlatformSubscriptionService subscriptionService;

    public PlatformRevenueService(SysRevenueRecordMapper revenueMapper, SysTenantMapper tenantMapper,
                                  PlatformAuditService auditService,
                                  PlatformSubscriptionService subscriptionService) {
        this.revenueMapper = revenueMapper;
        this.tenantMapper = tenantMapper;
        this.auditService = auditService;
        this.subscriptionService = subscriptionService;
    }

    public PageResult<RevenueVO> page(RevenueQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);
        Page<SysRevenueRecord> page = new Page<>(pageNo, size);
        Page<SysRevenueRecord> result = revenueMapper.selectPage(page, Wrappers.<SysRevenueRecord>lambdaQuery()
                .eq(query.tenantId() != null, SysRevenueRecord::getTenantId, query.tenantId())
                .eq(query.type() != null && !query.type().isBlank(), SysRevenueRecord::getType, query.type())
                .orderByDesc(SysRevenueRecord::getOccurredDate)
                .orderByDesc(SysRevenueRecord::getId));
        Map<Long, String> names = tenantNames(result.getRecords());
        List<RevenueVO> list = result.getRecords().stream().map(r -> toVO(r, names)).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    public RevenueSummaryVO summary(Long tenantId) {
        List<SysRevenueRecord> rows = revenueMapper.selectList(Wrappers.<SysRevenueRecord>lambdaQuery()
                .eq(tenantId != null, SysRevenueRecord::getTenantId, tenantId));
        BigDecimal payment = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        for (SysRevenueRecord r : rows) {
            BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
            if (TYPE_REFUND.equals(r.getType())) {
                refund = refund.add(amt);
            } else {
                payment = payment.add(amt);
            }
        }
        return new RevenueSummaryVO(payment, refund, payment.subtract(refund), rows.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(RevenueRecordDTO dto) {
        requireTenant(dto.tenantId());
        guardRefundNotExceedCollected(dto, null);
        SysRevenueRecord r = new SysRevenueRecord();
        apply(r, dto);
        revenueMapper.insert(r);
        auditService.record(PlatformAuditActions.REVENUE_SAVED, PlatformAuditActions.TARGET_REVENUE, r.getId(),
                Map.of("op", "create", "tenantId", dto.tenantId(), "amount", dto.amount()));
        return r.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RevenueRecordDTO dto) {
        SysRevenueRecord r = requireExists(id);
        requireTenant(dto.tenantId());
        guardRefundNotExceedCollected(dto, id);
        apply(r, dto);
        revenueMapper.updateById(r);
        auditService.record(PlatformAuditActions.REVENUE_SAVED, PlatformAuditActions.TARGET_REVENUE, id,
                Map.of("op", "update", "amount", dto.amount()));
    }

    /** 退款不得超过该租户已收净额（排除被编辑记录自身），防止产生负净额。 */
    private void guardRefundNotExceedCollected(RevenueRecordDTO dto, Long excludeId) {
        if (!TYPE_REFUND.equals(dto.type())) {
            return;
        }
        List<SysRevenueRecord> rows = revenueMapper.selectList(Wrappers.<SysRevenueRecord>lambdaQuery()
                .eq(SysRevenueRecord::getTenantId, dto.tenantId()));
        BigDecimal net = BigDecimal.ZERO;
        for (SysRevenueRecord r : rows) {
            if (excludeId != null && excludeId.equals(r.getId())) {
                continue;
            }
            BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
            net = TYPE_REFUND.equals(r.getType()) ? net.subtract(amt) : net.add(amt);
        }
        if (dto.amount().compareTo(net) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "退款金额不能超过该租户已收净额：" + net);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        revenueMapper.deleteById(id);
        auditService.record(PlatformAuditActions.REVENUE_DELETED, PlatformAuditActions.TARGET_REVENUE, id, null);
    }

    private void apply(SysRevenueRecord r, RevenueRecordDTO dto) {
        r.setTenantId(dto.tenantId());
        // 自动关联：未显式指定时取该租户当前生效订阅，便于按订阅对账
        if (dto.subscriptionId() != null) {
            r.setSubscriptionId(dto.subscriptionId());
        } else if (r.getSubscriptionId() == null) {
            SubscriptionVO sub = subscriptionService.currentSubscription(dto.tenantId());
            r.setSubscriptionId(sub == null ? null : sub.id());
        }
        r.setType(TYPE_REFUND.equals(dto.type()) ? TYPE_REFUND : TYPE_PAYMENT);
        r.setAmount(dto.amount());
        r.setCurrency(dto.currency() == null || dto.currency().isBlank() ? DEFAULT_CURRENCY : dto.currency());
        r.setContractNo(dto.contractNo());
        r.setOccurredDate(dto.occurredDate());
        r.setRemark(dto.remark());
    }

    private void requireTenant(Long tenantId) {
        if (tenantMapper.selectById(tenantId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
    }

    private SysRevenueRecord requireExists(Long id) {
        SysRevenueRecord r = revenueMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "收入记录不存在");
        }
        return r;
    }

    private Map<Long, String> tenantNames(List<SysRevenueRecord> rows) {
        List<Long> ids = rows.stream().map(SysRevenueRecord::getTenantId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            tenantMapper.selectBatchIds(ids).forEach(t -> names.put(t.getId(), t.getName()));
        }
        return names;
    }

    private RevenueVO toVO(SysRevenueRecord r, Map<Long, String> names) {
        return new RevenueVO(r.getId(), r.getTenantId(), names.get(r.getTenantId()), r.getSubscriptionId(),
                r.getType(), r.getAmount(), r.getCurrency(), r.getContractNo(), r.getOccurredDate(),
                r.getRemark(), r.getCreateTime());
    }
}
