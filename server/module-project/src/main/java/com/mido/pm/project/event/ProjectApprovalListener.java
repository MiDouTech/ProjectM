package com.mido.pm.project.event;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.service.ProjectInitService;
import com.mido.pm.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 监听立项审批结果事件，驱动项目状态机（事件解耦，审批域不直写项目表）：
 * - approval.approved(project_init) → 审批中→已注册（approvalPassed=true，写 pmo_registered_at）
 * - approval.withdrawn/rejected(project_init) → 审批中→草稿（撤回/驳回均可修改后重新提交）
 * AFTER_COMMIT：审批事务提交后才驱动，避免审批回滚却联动。
 */
@Component
public class ProjectApprovalListener {

    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalListener.class);

    private final ProjectService projectService;

    public ProjectApprovalListener(ProjectService projectService) {
        this.projectService = projectService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        boolean approved = "approval.approved".equals(message.eventType());
        // 撤回与驳回都让项目回退草稿，供修改后重新提交
        boolean backToDraft = "approval.withdrawn".equals(message.eventType())
                || "approval.rejected".equals(message.eventType());
        if (!approved && !backToDraft) {
            return;
        }
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        if (!ProjectInitService.BIZ_TYPE.equals(payload.get("bizType"))) {
            return;
        }
        Object bizId = payload.get("bizId");
        if (!(bizId instanceof Number num)) {
            return;
        }
        Long projectId = num.longValue();
        // AFTER_COMMIT：审批已提交，此处流转失败无法回滚审批，故显式 warn 而非静默吞掉
        // （项目若已非「审批中」会被状态机拒绝，记录便于排查，不再隐性丢失）
        try {
            if (approved) {
                log.info("立项审批通过，驱动项目注册：projectId={}", projectId);
                projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.REGISTERED.getCode(), true));
            } else {
                log.info("立项审批{}，项目回退草稿：projectId={}", message.eventType(), projectId);
                projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.DRAFT.getCode(), null));
            }
        } catch (BizException e) {
            log.warn("审批结果驱动项目流转被拒（项目状态可能已变更）：projectId={}, event={}, err={}",
                    projectId, message.eventType(), e.getMessage());
        }
    }
}
