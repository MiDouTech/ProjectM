package com.mido.pm.change.event;

import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 监听变更单审批结果（bizType=change），驱动变更生效/终结（事件解耦，审批域不直写变更表）：
 * - approval.approved → 应用变更（自动回写被改实体）
 * - approval.rejected/withdrawn → 变更单置驳回/撤回，实体不动
 * AFTER_COMMIT：审批事务提交后才驱动；镜像 ProjectApprovalListener。
 */
@Component
public class ChangeApprovalListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeApprovalListener.class);

    private final ChangeService changeService;

    public ChangeApprovalListener(ChangeService changeService) {
        this.changeService = changeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        boolean approved = "approval.approved".equals(message.eventType());
        boolean rejected = "approval.rejected".equals(message.eventType());
        boolean withdrawn = "approval.withdrawn".equals(message.eventType());
        if (!approved && !rejected && !withdrawn) {
            return;
        }
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        if (!ChangeService.APPROVAL_BIZ_TYPE.equals(payload.get("bizType"))) {
            return;
        }
        if (!(payload.get("bizId") instanceof Number num)) {
            return;
        }
        Long changeId = num.longValue();
        // AFTER_COMMIT：审批已提交，此处失败无法回滚审批，故 warn 而非静默
        try {
            if (approved) {
                log.info("变更审批通过，应用变更：changeId={}", changeId);
                changeService.onApprovalApproved(changeId);
            } else {
                log.info("变更审批{}，变更单终结：changeId={}", message.eventType(), changeId);
                changeService.onApprovalClosed(changeId, rejected);
            }
        } catch (BizException e) {
            log.warn("变更审批结果驱动失败：changeId={}, event={}, err={}",
                    changeId, message.eventType(), e.getMessage());
        }
    }
}
