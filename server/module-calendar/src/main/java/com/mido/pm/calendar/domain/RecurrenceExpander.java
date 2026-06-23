package com.mido.pm.calendar.domain;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 循环日程展开器（纯逻辑，无框架依赖）。
 *
 * <p>recur_rule 紧凑 JSON：{@code {"freq":"DAILY|WEEKLY|MONTHLY|YEARLY","interval":1,"count":10,"until":"2026-12-31"}}。
 * 每次步进同一规律（WEEKLY 取与首次相同的星期；多星期自定义 byWeekday 属 P2）。生成落在查询区间 [from,to]
 * 且与之时间重叠的实例，再套用例外（cancel 剔除 / modify 覆盖 start/end/title/location）。</p>
 */
public final class RecurrenceExpander {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceExpander.class);

    /** 单次区间内最多展开的实例数（已快进到窗口，此为窗口内实例上限）。 */
    private static final int MAX_OCCURRENCES = 1000;

    private RecurrenceExpander() {
    }

    public record Occurrence(LocalDateTime start, LocalDateTime end, String title,
                             String location, LocalDate occurrenceDate) {
    }

    /** 主记录是否为循环日程。 */
    public static boolean isRecurring(PmSchedule master) {
        return master.getRecurRule() != null && !master.getRecurRule().isBlank();
    }

    /**
     * 将循环主记录展开为区间 [from,to] 内的实例列表（已套用例外）。非循环主记录返回空。
     * 解析失败（recur_rule 非法）记日志并返回空，避免单条坏数据导致整次区间查询失败。
     */
    public static List<Occurrence> expand(PmSchedule master, List<PmScheduleException> exceptions,
                                          LocalDateTime from, LocalDateTime to) {
        List<Occurrence> result = new ArrayList<>();
        if (!isRecurring(master)) {
            return result;
        }
        String freq;
        int interval;
        Integer count;
        LocalDate until;
        try {
            JSONObject rule = JSONUtil.parseObj(master.getRecurRule());
            freq = rule.getStr("freq");
            if (freq == null || freq.isBlank()) {
                return result;
            }
            interval = Math.max(1, rule.getInt("interval", 1));
            Integer c = rule.getInt("count", -1);
            count = (c != null && c <= 0) ? null : c;
            until = rule.getStr("until") == null ? null : LocalDate.parse(rule.getStr("until"));
        } catch (Exception e) {
            log.warn("循环规则解析失败 scheduleId={} recurRule={}: {}",
                    master.getId(), master.getRecurRule(), e.getMessage());
            return result;
        }

        Duration duration = Duration.between(master.getStartTime(), master.getEndTime());
        Map<LocalDate, PmScheduleException> exMap = indexExceptions(exceptions);

        // 快进到查询窗口附近：避免从久远的起始日逐次步进、在到达窗口前撞上 MAX_OCCURRENCES 上限而漏掉实例。
        // skip 为完全早于 from 的整周期数（保守回退 1），idx 仍按全局序号计，使 count/until 语义不变。
        long skip = skipPeriods(master.getStartTime(), from, freq, interval);
        LocalDateTime occ = advance(master.getStartTime(), freq, skip * interval);

        for (long idx = skip; idx < skip + MAX_OCCURRENCES; idx++) {
            if (count != null && idx >= count) {
                break;
            }
            LocalDate occDate = occ.toLocalDate();
            if (until != null && occDate.isAfter(until)) {
                break;
            }
            if (occ.isAfter(to)) {
                break;
            }
            LocalDateTime occEnd = occ.plus(duration);
            if (occEnd.isAfter(from)) {
                Occurrence built = applyException(occ, occEnd, occDate, master, exMap.get(occDate));
                if (built != null) {
                    result.add(built);
                }
            }
            occ = step(occ, freq, interval);
        }
        return result;
    }

    /** 完全早于 windowStart 的整周期数（保守回退 1，由主循环的 occEnd&gt;from 过滤精确边界）。 */
    private static long skipPeriods(LocalDateTime start, LocalDateTime windowStart, String freq, int interval) {
        long between = switch (freq) {
            case "DAILY" -> ChronoUnit.DAYS.between(start, windowStart);
            case "WEEKLY" -> ChronoUnit.WEEKS.between(start, windowStart);
            case "MONTHLY" -> ChronoUnit.MONTHS.between(start, windowStart);
            case "YEARLY" -> ChronoUnit.YEARS.between(start, windowStart);
            default -> 0L;
        };
        if (between <= 0) {
            return 0;
        }
        return Math.max(0, between / interval - 1);
    }

    /** 从 start 前进 units 个时间单位（units = 周期数 × interval）。 */
    private static LocalDateTime advance(LocalDateTime start, String freq, long units) {
        return switch (freq) {
            case "DAILY" -> start.plusDays(units);
            case "WEEKLY" -> start.plusWeeks(units);
            case "MONTHLY" -> start.plusMonths(units);
            case "YEARLY" -> start.plusYears(units);
            default -> start;
        };
    }

    private static Occurrence applyException(LocalDateTime start, LocalDateTime end, LocalDate occDate,
                                             PmSchedule master, PmScheduleException ex) {
        String title = master.getTitle();
        String location = master.getLocation();
        if (ex != null) {
            if ("cancel".equals(ex.getAction())) {
                return null;
            }
            if (ex.getOverride() != null && !ex.getOverride().isBlank()) {
                JSONObject ov = JSONUtil.parseObj(ex.getOverride());
                if (ov.getStr("startTime") != null) {
                    start = LocalDateTime.parse(ov.getStr("startTime"));
                }
                if (ov.getStr("endTime") != null) {
                    end = LocalDateTime.parse(ov.getStr("endTime"));
                }
                if (ov.getStr("title") != null) {
                    title = ov.getStr("title");
                }
                if (ov.getStr("location") != null) {
                    location = ov.getStr("location");
                }
            }
        }
        return new Occurrence(start, end, title, location, occDate);
    }

    private static Map<LocalDate, PmScheduleException> indexExceptions(List<PmScheduleException> exceptions) {
        Map<LocalDate, PmScheduleException> map = new HashMap<>();
        if (exceptions != null) {
            for (PmScheduleException ex : exceptions) {
                map.put(ex.getOccurDate(), ex);
            }
        }
        return map;
    }

    private static LocalDateTime step(LocalDateTime occ, String freq, int interval) {
        return switch (freq) {
            case "DAILY" -> occ.plusDays(interval);
            case "WEEKLY" -> occ.plusWeeks(interval);
            case "MONTHLY" -> occ.plusMonths(interval);
            case "YEARLY" -> occ.plusYears(interval);
            default -> occ.plusYears(100); // 未知 freq：跳出循环
        };
    }
}
