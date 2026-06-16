package com.mido.pm.approval.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 条件求值（纯函数）：支持金额(数值)、项目类型(字符串)、职级(L 级数值)三类比较。
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static boolean evaluate(NodeCondition c, ApprovalContext ctx) {
        if (c == null) {
            return true;
        }
        Object actual = ctx.get(c.field());
        return switch (c.op()) {
            case "==" -> Objects.equals(asString(actual), c.value());
            case "!=" -> !Objects.equals(asString(actual), c.value());
            case ">", ">=", "<", "<=" -> compareNumeric(actual, c.value(), c.op());
            default -> false;
        };
    }

    private static boolean compareNumeric(Object actual, String value, String op) {
        BigDecimal a = toNumber(actual);
        BigDecimal b = toNumber(value);
        if (a == null || b == null) {
            return false;
        }
        int cmp = a.compareTo(b);
        return switch (op) {
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    /** 数值化：Number 直接用；"L3"→3；纯数字串→数值；否则 null。 */
    private static BigDecimal toNumber(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        String s = v.toString().trim();
        if (s.regionMatches(true, 0, "L", 0, 1) && s.length() > 1) {
            s = s.substring(1);
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }
}
