package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.common.security.JobLevelRule;
import org.springframework.stereotype.Component;

/**
 * 职级节点 guard：按项目类型配置的最低门槛 min_job_level 校验 Leader 职级，不满足直接拒绝。
 * 门槛由提交方放入审批上下文（取代原按 category 硬编码），复用 common {@link JobLevelRule}，单一事实源。
 */
@Component
public class JobLevelNodeGuard implements NodeGuard {

    public static final String KEY = "JOB_LEVEL";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public void check(FlowNode node, ApprovalContext ctx) {
        JobLevelRule.assertQualified(ctx.minJobLevel(), ctx.jobLevel());
    }
}
