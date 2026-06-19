package com.mido.pm.change.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.change.domain.ChangeStatus;
import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.entity.PmChangePolicy;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.change.event.ChangeEvents;
import com.mido.pm.change.mapper.PmChangePolicyMapper;
import com.mido.pm.change.mapper.PmChangeRequestMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 变更中心服务：提交变更单（复用审批引擎，bizType=change）、按策略决定必审/免审、台账查询、并发冻结判定。
 * 通过即回写（{@link ChangeApplyService}）。变更域不依赖被改业务域，回写经 ChangeApplier 端口。
 */
@Service
public class ChangeService {

    /** 提交到审批引擎时的 bizType（审批实例据此回流到变更域）。 */
    public static final String APPROVAL_BIZ_TYPE = "change";

    private final PmChangeRequestMapper changeMapper;
    private final PmChangePolicyMapper policyMapper;
    private final ApprovalService approvalService;
    private final ChangeApplyService applyService;
    private final DomainEventPublisher eventPublisher;

    public ChangeService(PmChangeRequestMapper changeMapper, PmChangePolicyMapper policyMapper,
                         ApprovalService approvalService, ChangeApplyService applyService,
                         DomainEventPublisher eventPublisher) {
        this.changeMapper = changeMapper;
        this.policyMapper = policyMapper;
        this.approvalService = approvalService;
        this.applyService = applyService;
        this.eventPublisher = eventPublisher;
    }

    /** 提交变更单：必审→走审批流；免审→即时回写。返回变更单 id。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(ChangeSubmitCmd cmd) {
        if (hasPending(cmd.bizType(), cmd.bizId())) {
            throw new BizException(ErrorCode.CONFLICT, "该对象已有进行中的变更单，请先完成或撤回");
        }
        PmChangeRequest cr = new PmChangeRequest();
        cr.setBizType(cmd.bizType());
        cr.setBizId(cmd.bizId());
        cr.setChangeType(cmd.changeType());
        cr.setTitle(cmd.title());
        cr.setReason(cmd.reason());
        cr.setImpact(cmd.impact());
        cr.setBeforeSnapshot(cmd.beforeSnapshot());
        cr.setAfterPayload(cmd.afterPayload());
        cr.setStatus(ChangeStatus.PENDING);
        try {
            changeMapper.insert(cr);
        } catch (DuplicateKeyException e) {
            // hasPending 预检与 insert 间的并发：DB 唯一索引 uk_pending 兜底，把竞态转成业务冲突而非 500
            throw new BizException(ErrorCode.CONFLICT, "该对象已有进行中的变更单，请先完成或撤回");
        }

        PmChangePolicy policy = resolvePolicy(cmd.changeType());
        boolean requireApproval = policy != null && Integer.valueOf(1).equals(policy.getRequireApproval());
        if (requireApproval) {
            if (policy.getFlowId() == null) {
                throw new BizException(ErrorCode.CONFLICT,
                        "变更类型「" + cmd.changeType() + "」要求审批但未绑定审批流，请先在变更策略中配置");
            }
            Map<String, Object> formData = cmd.formData() != null
                    ? new LinkedHashMap<>(cmd.formData()) : new LinkedHashMap<>();
            formData.putIfAbsent("changeType", cmd.changeType());
            formData.putIfAbsent("changeTitle", cmd.title());
            Long instanceId = approvalService.submit(
                    new SubmitDTO(policy.getFlowId(), APPROVAL_BIZ_TYPE, cr.getId(), formData));
            cr.setApprovalInstanceId(instanceId);
            changeMapper.updateById(cr);
            eventPublisher.publish(ChangeEvents.REQUESTED, requestedPayload(cr, true));
        } else {
            // 免审：仅留痕 + 即时回写
            eventPublisher.publish(ChangeEvents.REQUESTED, requestedPayload(cr, false));
            applyService.apply(cr.getId());
        }
        return cr.getId();
    }

    /** 审批通过回流：应用变更（自动回写模式）。 */
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalApproved(Long changeId) {
        applyService.apply(changeId);
    }

    /** 审批驳回/撤回回流：置终态，被改实体不动。 */
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalClosed(Long changeId, boolean rejected) {
        PmChangeRequest cr = changeMapper.selectById(changeId);
        if (cr == null || !ChangeStatus.PENDING.equals(cr.getStatus())) {
            return;
        }
        cr.setStatus(rejected ? ChangeStatus.REJECTED : ChangeStatus.WITHDRAWN);
        changeMapper.updateById(cr);
        eventPublisher.publish(ChangeEvents.REJECTED, requestedPayload(cr, true));
    }

    /** 并发冻结判定：对象是否存在进行中(pending)的变更单。 */
    public boolean hasPending(String bizType, Long bizId) {
        Long n = changeMapper.selectCount(Wrappers.<PmChangeRequest>lambdaQuery()
                .eq(PmChangeRequest::getBizType, bizType)
                .eq(PmChangeRequest::getBizId, bizId)
                .eq(PmChangeRequest::getStatus, ChangeStatus.PENDING));
        return n != null && n > 0;
    }

    /** 变更中心台账：按 bizType/bizId/status 可选过滤，倒序。 */
    public List<ChangeRequestVO> list(String bizType, Long bizId, String status) {
        return changeMapper.selectList(Wrappers.<PmChangeRequest>lambdaQuery()
                        .eq(bizType != null, PmChangeRequest::getBizType, bizType)
                        .eq(bizId != null, PmChangeRequest::getBizId, bizId)
                        .eq(status != null, PmChangeRequest::getStatus, status)
                        .orderByDesc(PmChangeRequest::getId))
                .stream().map(this::toVO).toList();
    }

    public ChangeRequestVO get(Long id) {
        PmChangeRequest cr = changeMapper.selectById(id);
        if (cr == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "变更单不存在");
        }
        return toVO(cr);
    }

    private PmChangePolicy resolvePolicy(String changeType) {
        return policyMapper.selectList(Wrappers.<PmChangePolicy>lambdaQuery()
                        .eq(PmChangePolicy::getChangeType, changeType)
                        .eq(PmChangePolicy::getEnabled, 1))
                .stream().findFirst().orElse(null);
    }

    private Map<String, Object> requestedPayload(PmChangeRequest cr, boolean requireApproval) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("changeId", cr.getId());
        m.put("bizType", cr.getBizType());
        m.put("bizId", cr.getBizId());
        m.put("changeType", cr.getChangeType());
        m.put("status", cr.getStatus());
        m.put("requireApproval", requireApproval);
        return m;
    }

    private ChangeRequestVO toVO(PmChangeRequest cr) {
        return new ChangeRequestVO(cr.getId(), cr.getBizType(), cr.getBizId(), cr.getChangeType(),
                cr.getTitle(), cr.getReason(), cr.getImpact(), cr.getBeforeSnapshot(), cr.getAfterPayload(),
                cr.getStatus(), cr.getApprovalInstanceId(), cr.getAppliedAt(), cr.getCreateBy(), cr.getCreateTime());
    }
}
