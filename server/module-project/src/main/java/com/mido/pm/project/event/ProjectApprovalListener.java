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
 * 监听 approval.approved(biz_type=project_init) → 驱动项目 审批中→已注册（写 pmo_registered_at）。
 * 事件解耦：审批域只发事件，注册由项目状态机完成（approvalPassed=true）。
 * AFTER_COMMIT：审批事务提交后才注册，避免审批回滚却注册。
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
        if (!"approval.approved".equals(message.eventType())) {
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
        log.info("立项审批通过，驱动项目注册：projectId={}", projectId);
        projectService.transition(projectId, new ProjectTransitionDTO(ProjectStatus.REGISTERED.getCode(), true));
    }
}
