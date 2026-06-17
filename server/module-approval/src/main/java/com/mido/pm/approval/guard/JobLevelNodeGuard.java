package com.mido.pm.approval.guard;

import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.common.security.JobLevelRule;
import org.springframework.stereotype.Component;

/**
 * 职级节点 guard（npss-rule §8）：S 类 Leader 必须 L3+、O 类 L2+，不满足直接拒绝。
 * 复用 common {@link JobLevelRule}（与项目状态机注册 guard 同一事实源）。
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
        JobLevelRule.assertQualified(ctx.category(), ctx.jobLevel());
    }
}
