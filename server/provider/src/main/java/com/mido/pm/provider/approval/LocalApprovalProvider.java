package com.mido.pm.provider.approval;

import com.mido.pm.common.util.Ids;
import org.springframework.stereotype.Component;

/**
 * 本地审批 Provider 占位实现。阶段一走本地审批引擎，企微审批实现预留。
 */
@Component
public class LocalApprovalProvider implements ApprovalProvider {

    @Override
    public Long startApproval(String bizType, Long bizId, Long applicantId) {
        // TODO 阶段二接入 approval_instance / approval_flow
        return Ids.nextId();
    }
}
