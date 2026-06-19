package com.mido.pm.change.service;

import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.domain.ChangeStatus;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.change.event.ChangeEvents;
import com.mido.pm.change.mapper.PmChangeRequestMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 变更应用：把通过的变更单经对应 {@link ChangeApplier}（被改域实现）回写实体，置 applied 并发 change.applied。
 * 应用与归档同事务；幂等（已 applied 直接返回，防审批回调重入）。
 */
@Service
public class ChangeApplyService {

    private final PmChangeRequestMapper changeMapper;
    private final List<ChangeApplier> appliers;
    private final DomainEventPublisher eventPublisher;

    public ChangeApplyService(PmChangeRequestMapper changeMapper, List<ChangeApplier> appliers,
                              DomainEventPublisher eventPublisher) {
        this.changeMapper = changeMapper;
        this.appliers = appliers;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void apply(Long changeId) {
        PmChangeRequest cr = changeMapper.selectById(changeId);
        if (cr == null || ChangeStatus.APPLIED.equals(cr.getStatus())) {
            return; // 幂等
        }
        ChangeApplier applier = appliers.stream()
                .filter(a -> a.supports(cr.getBizType()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.CONFLICT,
                        "无变更应用器处理 bizType=" + cr.getBizType()));
        applier.apply(cr);
        cr.setStatus(ChangeStatus.APPLIED);
        cr.setAppliedAt(LocalDateTime.now());
        changeMapper.updateById(cr);
        eventPublisher.publish(ChangeEvents.APPLIED, payload(cr));
    }

    private Map<String, Object> payload(PmChangeRequest cr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("changeId", cr.getId());
        m.put("bizType", cr.getBizType());
        m.put("bizId", cr.getBizId());
        m.put("changeType", cr.getChangeType());
        return m;
    }
}
