package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;

/**
 * 可插拔节点 guard。节点流转到该节点（或节点通过）前调用，不满足直接拒绝。
 * 实现注册为 Bean，按 {@link #key()} 与节点 definition 的 guard 标识匹配。
 */
public interface NodeGuard {

    /** guard 标识，对应节点 definition 的 guard 字段。 */
    String key();

    /** 校验；不满足抛 BizException。 */
    void check(FlowNode node, ApprovalContext ctx);
}
