package com.mido.pm.approval.outcome;

import com.mido.pm.approval.event.ApprovalEvents;
import com.mido.pm.common.outbox.DomainEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 审批结果统一路由（唯一 listener）：监听 approval.approved/rejected/withdrawn，一处完成
 * 事件解码 + payload guard + bizId 提取 + 异常兜底，按 bizType 路由到对应
 * {@link ApprovalOutcomeHandler}。取代原 project/cost/change 各自重复的 {@code *ApprovalListener}。
 *
 * <p>AFTER_COMMIT：审批事务提交后才驱动（避免审批回滚却联动）；回写异常不得逸出
 * （已提交不可回滚、无重投），统一记错便于人工/补偿处理。各域若有「良性可忽略」异常
 * （如项目状态已变更），在自己的 handler 内处理后正常返回。
 */
@Component
public class ApprovalOutcomeRouter {

    private static final Logger log = LoggerFactory.getLogger(ApprovalOutcomeRouter.class);

    private final Map<String, ApprovalOutcomeHandler> handlers;

    public ApprovalOutcomeRouter(List<ApprovalOutcomeHandler> handlerList) {
        // 同一 bizType 重复注册即启动期失败（toMap 抛错），避免回写歧义
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(ApprovalOutcomeHandler::bizType, Function.identity()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        String type = message.eventType();
        boolean approved = ApprovalEvents.APPROVED.equals(type);
        boolean rejected = ApprovalEvents.REJECTED.equals(type);
        boolean withdrawn = ApprovalEvents.WITHDRAWN.equals(type);
        if (!approved && !rejected && !withdrawn) {
            return;
        }
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        Object bizType = payload.get("bizType");
        ApprovalOutcomeHandler handler = bizType == null ? null : handlers.get(bizType.toString());
        if (handler == null) {
            return;
        }
        if (!(payload.get("bizId") instanceof Number num)) {
            return;
        }
        long bizId = num.longValue();
        try {
            if (approved) {
                handler.onApproved(bizId);
            } else if (rejected) {
                handler.onRejected(bizId);
            } else {
                handler.onWithdrawn(bizId);
            }
        } catch (Exception e) {
            log.error("审批结果驱动失败(bizType={}, bizId={}, event={})，业务可能滞留，需人工介入",
                    bizType, bizId, type, e);
        }
    }
}
