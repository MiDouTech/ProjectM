package com.mido.pm.provider.approval;

/**
 * 企微审批 Provider（TODO·P2 激活）：企微审批 API。
 * 阶段一不注册为 Bean，占位预留。
 */
public class WecomApprovalProvider implements ApprovalProvider {

    @Override
    public Long startApproval(String bizType, Long bizId, Long applicantId) {
        throw new UnsupportedOperationException("TODO: 企微审批实现，P2 激活");
    }
}
