package com.mido.pm.provider.approval;

/**
 * 审批 Provider 接口。屏蔽底层审批引擎（本地审批 / 企微审批）。
 */
public interface ApprovalProvider {

    /**
     * 发起一个审批实例。
     *
     * @param bizType    业务类型（如 project / cost）
     * @param bizId      业务主键
     * @param applicantId 申请人
     * @return 审批实例 ID
     */
    Long startApproval(String bizType, Long bizId, Long applicantId);
}
