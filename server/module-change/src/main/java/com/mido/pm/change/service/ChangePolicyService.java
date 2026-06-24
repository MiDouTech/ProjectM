package com.mido.pm.change.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.change.dto.ChangePolicyUpsertDTO;
import com.mido.pm.change.dto.ChangePolicyVO;
import com.mido.pm.change.entity.PmChangePolicy;
import com.mido.pm.change.mapper.PmChangePolicyMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 变更策略管理：各变更类型「必审/免审 + 绑定审批流」的查询与配置（租户自配）。
 * 必审硬校验：require_approval=1 必须绑 flowId，否则保存即拒——从源头杜绝「必审但无流」导致提交变更 500。
 */
@Service
public class ChangePolicyService {

    private final PmChangePolicyMapper policyMapper;

    public ChangePolicyService(PmChangePolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

    public List<ChangePolicyVO> list() {
        return policyMapper.selectList(Wrappers.<PmChangePolicy>lambdaQuery()
                        .orderByAsc(PmChangePolicy::getChangeType))
                .stream()
                .map(p -> new ChangePolicyVO(p.getId(), p.getChangeType(),
                        p.getRequireApproval(), p.getFlowId(), p.getEnabled()))
                .toList();
    }

    /** 按 changeType 幂等保存：存在则更新，否则新增。必审需绑流。 */
    @Transactional(rollbackFor = Exception.class)
    public void save(ChangePolicyUpsertDTO dto) {
        boolean requireApproval = Integer.valueOf(1).equals(dto.requireApproval());
        if (requireApproval && dto.flowId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "必审策略须绑定审批流");
        }
        PmChangePolicy existing = policyMapper.selectOne(Wrappers.<PmChangePolicy>lambdaQuery()
                .eq(PmChangePolicy::getChangeType, dto.changeType())
                .last("limit 1"));
        PmChangePolicy p = existing != null ? existing : new PmChangePolicy();
        p.setChangeType(dto.changeType());
        p.setRequireApproval(requireApproval ? 1 : 0);
        p.setFlowId(requireApproval ? dto.flowId() : null);
        p.setEnabled(Integer.valueOf(0).equals(dto.enabled()) ? 0 : 1);
        if (existing != null) {
            policyMapper.updateById(p);
        } else {
            policyMapper.insert(p);
        }
    }
}
