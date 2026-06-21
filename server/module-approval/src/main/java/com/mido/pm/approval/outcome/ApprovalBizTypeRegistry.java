package com.mido.pm.approval.outcome;

import com.mido.pm.approval.dto.ApprovalBizTypeVO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 审批 bizType 单一信息源：由各 {@link ApprovalOutcomeHandler} 自登记 code/label/order 汇成。
 * {@link ApprovalOutcomeRouter} 用它按 bizType 查 handler；Controller 用它 list 暴露给前端，
 * 消除「后端常量 + 前端字典」双写漂移。
 */
@Component
public class ApprovalBizTypeRegistry {

    private final Map<String, ApprovalOutcomeHandler> byCode;
    private final List<ApprovalBizTypeVO> list;

    public ApprovalBizTypeRegistry(List<ApprovalOutcomeHandler> handlers) {
        // 同一 bizType 重复注册即启动期失败（toMap 抛错），避免回写/标签歧义
        this.byCode = handlers.stream()
                .collect(Collectors.toMap(ApprovalOutcomeHandler::bizType, Function.identity()));
        this.list = handlers.stream()
                .sorted(Comparator.comparingInt(ApprovalOutcomeHandler::order)
                        .thenComparing(ApprovalOutcomeHandler::bizType))
                .map(h -> new ApprovalBizTypeVO(h.bizType(), h.label()))
                .toList();
    }

    /** 按 bizType 取处理器，无则 null。 */
    public ApprovalOutcomeHandler handler(String bizType) {
        return bizType == null ? null : byCode.get(bizType);
    }

    /** 已注册的 bizType 列表（code+label，按 order 稳定排序），供前端消费。 */
    public List<ApprovalBizTypeVO> list() {
        return list;
    }
}
