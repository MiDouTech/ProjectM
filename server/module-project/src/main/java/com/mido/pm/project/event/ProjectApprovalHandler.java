package com.mido.pm.project.event;

import com.mido.pm.approval.outcome.ApprovalOutcomeHandler;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.service.ProjectInitService;
import com.mido.pm.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 立项审批结果驱动项目状态机：通过→已注册（写 pmo_registered_at）；驳回/撤回→回草稿。
 *
 * <p>项目若已非「审批中」会被状态机以 {@link BizException} 拒绝，属预期良性，记 warn 不丢失；
 * 非 Biz 异常上抛交由 {@link com.mido.pm.approval.outcome.ApprovalOutcomeRouter} 兜底。
 */
@Component
public class ProjectApprovalHandler implements ApprovalOutcomeHandler {

    private static final Logger log = LoggerFactory.getLogger(ProjectApprovalHandler.class);

    private final ProjectService projectService;

    public ProjectApprovalHandler(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public String bizType() {
        return ProjectInitService.BIZ_TYPE;
    }

    @Override
    public void onApproved(long bizId) {
        log.info("立项审批通过，驱动项目注册：projectId={}", bizId);
        transition(bizId, new ProjectTransitionDTO(ProjectStatus.REGISTERED.getCode(), true));
    }

    @Override
    public void onRejected(long bizId) {
        backToDraft(bizId);
    }

    @Override
    public void onWithdrawn(long bizId) {
        backToDraft(bizId);
    }

    private void backToDraft(long bizId) {
        log.info("立项审批驳回/撤回，项目回退草稿：projectId={}", bizId);
        transition(bizId, new ProjectTransitionDTO(ProjectStatus.DRAFT.getCode(), null));
    }

    private void transition(long projectId, ProjectTransitionDTO dto) {
        // BizException = 项目状态已变更（良性），warn 便于排查；非 Biz 异常上抛由 Router 记错兜底
        try {
            projectService.transition(projectId, dto);
        } catch (BizException e) {
            log.warn("审批结果驱动项目流转被拒（项目状态可能已变更）：projectId={}, err={}",
                    projectId, e.getMessage());
        }
    }
}
