package com.mido.pm.project.event;

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
 * - approval.withdrawn(project_init) → 审批中→草稿（发起人撤回，可修改后重新提交）
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
        boolean withdrawn = "approval.withdrawn".equals(message.eventType());
        if (!approved && !withdrawn) {
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
        if (approved) {
            log.info("立项审批通过，驱动项目注册：projectId={}", projectId);
            projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.REGISTERED.getCode(), true));
        } else {
            log.info("立项审批撤回，项目回退草稿：projectId={}", projectId);
            projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.DRAFT.getCode(), null));
        }
    }
}
