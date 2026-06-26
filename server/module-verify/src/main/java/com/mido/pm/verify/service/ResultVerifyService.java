package com.mido.pm.verify.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.common.verify.ResultVerifyGate;
import com.mido.pm.verify.dto.ResultVerifySaveDTO;
import com.mido.pm.verify.dto.ResultVerifyVO;
import com.mido.pm.verify.entity.PmResultVerify;
import com.mido.pm.verify.event.ResultVerifyEvents;
import com.mido.pm.verify.mapper.PmResultVerifyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结果验收（铁三角）：录入 PMO 结论 + 实现结案闸门 {@link ResultVerifyGate}。
 * 「结果验收 → 已结案」前置硬校验：无达标(pass)结论拒绝结案（架构 §2.2 严肃闸门）。
 */
@Service
public class ResultVerifyService implements ResultVerifyGate {

    /** 达标结论值 */
    public static final String VERDICT_PASS = "pass";

    private final PmResultVerifyMapper mapper;
    private final DomainEventPublisher eventPublisher;

    public ResultVerifyService(PmResultVerifyMapper mapper, DomainEventPublisher eventPublisher) {
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    /** 项目最新一条结果验收结论；无则 null。 */
    public ResultVerifyVO latest(Long projectId) {
        PmResultVerify e = latestEntity(projectId);
        return e == null ? null : toVO(e);
    }

    /** 录入结果验收结论（每次一条，最新为权威）。同事务写 Outbox 事件。 */
    @Transactional(rollbackFor = Exception.class)
    public ResultVerifyVO save(Long projectId, ResultVerifySaveDTO dto) {
        PmResultVerify e = new PmResultVerify();
        e.setProjectId(projectId);
        e.setVerdict(dto.verdict());
        e.setOnTime(toFlag(dto.onTime()));
        e.setInBudget(toFlag(dto.inBudget()));
        e.setInScope(toFlag(dto.inScope()));
        e.setCompletionRate(dto.completionRate());
        e.setRemark(dto.remark());
        e.setVerifiedBy(UserContext.currentUserId());
        e.setVerifiedAt(LocalDateTime.now());
        mapper.insert(e);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", projectId);
        payload.put("verdict", e.getVerdict());
        payload.put("resultVerifyId", e.getId());
        eventPublisher.publish(ResultVerifyEvents.VERIFIED, payload);
        return toVO(e);
    }

    @Override
    public void assertClosable(Long projectId) {
        PmResultVerify e = latestEntity(projectId);
        if (e == null || !VERDICT_PASS.equals(e.getVerdict())) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "项目须通过结果验收（铁三角达标）方可结案");
        }
    }

    private PmResultVerify latestEntity(Long projectId) {
        return mapper.selectOne(Wrappers.<PmResultVerify>lambdaQuery()
                .eq(PmResultVerify::getProjectId, projectId)
                .orderByDesc(PmResultVerify::getId)
                .last("limit 1"));
    }

    private Integer toFlag(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }

    private ResultVerifyVO toVO(PmResultVerify e) {
        return new ResultVerifyVO(e.getId(), e.getProjectId(), e.getVerdict(),
                toBool(e.getOnTime()), toBool(e.getInBudget()), toBool(e.getInScope()),
                e.getCompletionRate(), e.getRemark(), e.getVerifiedBy(), e.getVerifiedAt());
    }

    private Boolean toBool(Integer i) {
        return i != null && i == 1;
    }
}
