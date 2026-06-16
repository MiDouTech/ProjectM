package com.mido.pm.project.domain;

import com.mido.pm.common.security.JobLevelRule;

/**
 * 立项职级 guard（npss-rule §8）。委托 common {@link JobLevelRule}（与审批节点 guard 共用单一事实源）。
 */
public final class JobLevelGuard {

    private JobLevelGuard() {
    }

    /** 校验 Leader 职级是否满足项目类型门槛，不满足抛 BizException(403)。 */
    public static void assertLeaderQualified(ProjectCategory category, String jobLevel) {
        JobLevelRule.assertQualified(category.getCode(), jobLevel);
    }
}
