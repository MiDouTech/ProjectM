package com.mido.pm.project.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 骨架供给的占位实现（严格分层、延后落地）。
 * 仅记录待供给的骨架；任务/干系人/审批骨架待 Step 3/4/5 各域实现替换/扩展本端口。
 */
@Component
public class NoopProjectSkeletonProvisioner implements ProjectSkeletonProvisioner {

    private static final Logger log = LoggerFactory.getLogger(NoopProjectSkeletonProvisioner.class);

    @Override
    public void provision(Long projectId, TemplateConfig config) {
        int phaseCount = config.phases() == null ? 0 : config.phases().size();
        int stakeholderCount = config.stakeholders() == null ? 0 : config.stakeholders().size();
        log.info("[骨架占位] project={} 待供给：阶段{}个/干系人{}个/审批流={}（任务·干系人·审批由 Step3/4/5 落地）",
                projectId, phaseCount, stakeholderCount, config.approvalFlow());
    }
}
