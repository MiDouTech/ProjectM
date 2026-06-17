package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 节点 guard 注册表：按 key 聚合所有 {@link NodeGuard} Bean，可插拔。
 * 节点激活时按其 definition 的 guard 标识执行校验。
 */
@Component
public class NodeGuardRegistry {

    private final Map<String, NodeGuard> guards;

    public NodeGuardRegistry(List<NodeGuard> guardList) {
        this.guards = guardList.stream().collect(Collectors.toMap(NodeGuard::key, Function.identity()));
    }

    /** 执行节点 guard；无 guard 则跳过，未知 guard 报错。 */
    public void run(FlowNode node, ApprovalContext ctx) {
        String key = node.guard();
        if (key == null || key.isBlank()) {
            return;
        }
        NodeGuard guard = guards.get(key);
        if (guard == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "未知节点 guard: " + key);
        }
        guard.check(node, ctx);
    }
}
