package com.mido.pm.project.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

/**
 * 立项职级 guard（npss-rule §8）：S 类 Leader 必须 L3+，O 类 L2+，I 类不限。
 * 纯逻辑，可单测。职级格式形如 L1/L2/L3。
 */
public final class JobLevelGuard {

    private JobLevelGuard() {
    }

    /** 校验 Leader 职级是否满足项目类型门槛，不满足抛 {@link BizException}(403)。 */
    public static void assertLeaderQualified(ProjectCategory category, String jobLevel) {
        int required = requiredLevel(category);
        if (required == 0) {
            return;
        }
        int actual = parseLevel(jobLevel);
        if (actual < required) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    category.getCode() + " 类项目 Leader 职级需 L" + required + "+，当前 "
                            + (jobLevel == null || jobLevel.isBlank() ? "无" : jobLevel));
        }
    }

    private static int requiredLevel(ProjectCategory category) {
        return switch (category) {
            case S -> 3;
            case O -> 2;
            case I -> 0;
        };
    }

    /** 解析 "L3" → 3；无法解析（null/空/非法）→ 0。 */
    private static int parseLevel(String jobLevel) {
        if (jobLevel == null) {
            return 0;
        }
        String s = jobLevel.trim().toUpperCase();
        if (s.startsWith("L")) {
            s = s.substring(1);
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
