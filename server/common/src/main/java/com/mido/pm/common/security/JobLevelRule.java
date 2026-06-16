package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

/**
 * 立项职级规则（npss-rule §8）：S 类需 L3+，O 类需 L2+，I 类不限。纯逻辑、可单测、跨域共用。
 * 项目状态机注册 guard 与审批节点 guard 均复用本规则，单一事实源。职级格式形如 L1/L2/L3。
 */
public final class JobLevelRule {

    private JobLevelRule() {
    }

    /** 项目类型对应的最低职级数；非 S/O 返回 0（不限）。 */
    public static int requiredLevel(String categoryCode) {
        if (categoryCode == null) {
            return 0;
        }
        return switch (categoryCode) {
            case "S" -> 3;
            case "O" -> 2;
            default -> 0;
        };
    }

    /** 解析 "L3" → 3；null/空/非法 → 0。 */
    public static int parseLevel(String jobLevel) {
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

    public static boolean qualifies(String categoryCode, String jobLevel) {
        return parseLevel(jobLevel) >= requiredLevel(categoryCode);
    }

    /** 校验职级达标，不满足抛 {@link BizException}(403) 并给清晰错误。 */
    public static void assertQualified(String categoryCode, String jobLevel) {
        int required = requiredLevel(categoryCode);
        if (parseLevel(jobLevel) < required) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    categoryCode + " 类需 Leader 职级 L" + required + "+，当前 "
                            + (jobLevel == null || jobLevel.isBlank() ? "无" : jobLevel));
        }
    }
}
