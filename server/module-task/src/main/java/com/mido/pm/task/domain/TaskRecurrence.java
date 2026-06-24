package com.mido.pm.task.domain;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

import java.time.LocalDate;
import java.util.Set;

/**
 * 任务循环规则（纯逻辑，无框架依赖）。
 *
 * <p>recur_rule 紧凑 JSON，与日历日程同构：
 * {@code {"freq":"DAILY|WEEKLY|MONTHLY|YEARLY","interval":1,"count":10,"until":"2026-12-31"}}。
 * 语义：模板任务自身计为第 1 次出现；{@code count} 为含模板在内的总次数（故实例数 = count-1）；
 * {@code until} 为最后一次出现日期上限（含）。仅按固定周期步进，多星期 byWeekday 暂不支持。</p>
 */
public record TaskRecurrence(String freq, int interval, Integer count, LocalDate until) {

    private static final Set<String> FREQS = Set.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");

    /**
     * 解析并校验循环规则。空白返回 null（=非循环）；格式非法抛 {@link BizException}（PARAM_ERROR）。
     */
    public static TaskRecurrence parse(String rule) {
        if (rule == null || rule.isBlank()) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(rule);
            String freq = obj.getStr("freq");
            if (freq == null || !FREQS.contains(freq)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "循环规则 freq 非法（须为 DAILY/WEEKLY/MONTHLY/YEARLY）");
            }
            int interval = Math.max(1, obj.getInt("interval", 1));
            Integer c = obj.getInt("count", -1);
            Integer count = (c != null && c <= 0) ? null : c;
            LocalDate until = obj.getStr("until") == null ? null : LocalDate.parse(obj.getStr("until"));
            return new TaskRecurrence(freq, interval, count, until);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "循环规则格式非法: " + e.getMessage());
        }
    }

    /** 以 base 为第 1 次出现，返回第 (1+times) 次出现的日期（times≥1）。 */
    public LocalDate shift(LocalDate base, int times) {
        long step = (long) interval * times;
        return switch (freq) {
            case "DAILY" -> base.plusDays(step);
            case "WEEKLY" -> base.plusWeeks(step);
            case "MONTHLY" -> base.plusMonths(step);
            case "YEARLY" -> base.plusYears(step);
            default -> base;
        };
    }
}
