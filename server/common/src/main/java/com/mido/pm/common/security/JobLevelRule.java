package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

/**
 * 立项职级规则（门槛制，纯逻辑、可单测、跨域共用）。
 * 门槛由「项目类型」属性 min_job_level 提供（取代原硬编码 S→L3/O→L2/I 不限），单一事实源。
 * 项目状态机注册 guard 与审批节点 guard 均复用本规则。职级/门槛格式形如 L1/L2/L3，空=不限。
 */
public final class JobLevelRule {

    private JobLevelRule() {
    }

    /** 解析 "L3"→3；null/空/非法→0（0 即不限/无）。 */
    public static int parseLevel(String level) {
        if (level == null) {
            return 0;
        }
        String s = level.trim().toUpperCase();
        if (s.startsWith("L")) {
            s = s.substring(1);
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 是否达标：实际职级 ≥ 最低门槛（minJobLevel 空=不限，恒达标）。 */
    public static boolean qualifies(String minJobLevel, String jobLevel) {
        return parseLevel(jobLevel) >= parseLevel(minJobLevel);
    }

    /** 校验职级达标，不满足抛 {@link BizException}(403) 并给清晰错误。minJobLevel 空=不限。 */
    public static void assertQualified(String minJobLevel, String jobLevel) {
        int required = parseLevel(minJobLevel);
        if (parseLevel(jobLevel) < required) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "立项 Leader 职级需 L" + required + "+，当前 "
                            + (jobLevel == null || jobLevel.isBlank() ? "无" : jobLevel));
        }
    }
}
