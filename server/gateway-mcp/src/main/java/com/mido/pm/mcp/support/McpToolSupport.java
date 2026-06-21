package com.mido.pm.mcp.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具公共辅助：入参解析（容忍 JSON 的 Integer/Long/字符串混入）与出参封装（VO → JSON 文本结果）。
 */
public final class McpToolSupport {

    private McpToolSupport() {
    }

    /** 取必填 Long 入参，缺失或非法抛 {@link IllegalArgumentException}（由工具转为 isError 结果）。 */
    public static long requireLong(Map<String, Object> args, String key) {
        Long v = optLong(args, key);
        if (v == null) {
            throw new IllegalArgumentException("缺少必填参数：" + key);
        }
        return v;
    }

    /** 取可选 Long 入参，缺失返回 null。 */
    public static Long optLong(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数 " + key + " 不是合法整数：" + s);
        }
    }

    /** 取必填 String 入参，缺失或空白抛 {@link IllegalArgumentException}。 */
    public static String requireString(Map<String, Object> args, String key) {
        String v = optString(args, key);
        if (v == null) {
            throw new IllegalArgumentException("缺少必填参数：" + key);
        }
        return v;
    }

    /** 取可选 Integer 入参，缺失返回 null。 */
    public static Integer optInteger(Map<String, Object> args, String key) {
        Long v = optLong(args, key);
        return v == null ? null : v.intValue();
    }

    /** 取可选 ISO-8601 日期入参（如 2026-06-30），缺失返回 null，格式非法抛 {@link IllegalArgumentException}。 */
    public static LocalDate optLocalDate(Map<String, Object> args, String key) {
        String s = optString(args, key);
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("参数 " + key + " 不是合法日期(YYYY-MM-DD)：" + s);
        }
    }

    /** 取可选 String 入参，缺失或空白返回 null。 */
    public static String optString(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 取可选 Long 列表入参（如 @用户 ID 数组），缺失返回 null，元素非法抛 {@link IllegalArgumentException}。 */
    public static List<Long> optLongList(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof List<?> raw)) {
            throw new IllegalArgumentException("参数 " + key + " 需为数组");
        }
        List<Long> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof Number n) {
                result.add(n.longValue());
            } else if (item != null) {
                try {
                    result.add(Long.valueOf(item.toString().trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("参数 " + key + " 含非法整数：" + item);
                }
            }
        }
        return result;
    }

    /** 取可选 Boolean 入参，缺失返回 null。 */
    public static Boolean optBoolean(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.valueOf(v.toString().trim());
    }

    /** 把业务对象序列化为 JSON 文本，封装为成功的工具结果。 */
    public static CallToolResult ok(ObjectMapper objectMapper, Object data) {
        try {
            return new CallToolResult(objectMapper.writeValueAsString(data), false);
        } catch (Exception e) {
            return new CallToolResult("结果序列化失败：" + e.getMessage(), true);
        }
    }

    /** 封装为错误的工具结果（返回给智能体而非抛异常，便于其自行处置）。 */
    public static CallToolResult error(String message) {
        return new CallToolResult(message, true);
    }
}
