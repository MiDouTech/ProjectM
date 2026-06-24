package com.mido.pm.task.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.dto.TaskVO;
import com.mido.pm.view.dto.ViewConfig;
import com.mido.pm.view.dto.ViewConfig.FilterCondition;
import com.mido.pm.view.dto.ViewConfig.FilterGroup;
import com.mido.pm.view.dto.ViewConfig.SortSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视图中自定义字段（{@code cf:<fieldKey>}）的支持：纯函数，便于单测。
 *
 * <p>原生字段仍走 {@link ViewQueryTranslator} 下推 SQL；自定义字段值在 EAV，
 * 按 A 内存方案——在已加载（≤上限）的结果集上做筛选与排序。本类负责：</p>
 * <ol>
 *   <li>{@link #split} 把 ViewConfig 拆成「下推 SQL 的原生部分」与「内存处理的 cf 部分」；</li>
 *   <li>{@link #matches} 内存匹配单条任务（原生 + cf 通用）；</li>
 *   <li>{@link #comparator} 多级排序（原生 + cf 通用）。</li>
 * </ol>
 */
public final class TaskViewCustomField {

    /** 自定义字段列/字段引用前缀。 */
    public static final String PREFIX = "cf:";

    private TaskViewCustomField() {
    }

    public static boolean isCf(String field) {
        return field != null && field.startsWith(PREFIX);
    }

    public static String key(String field) {
        return field.substring(PREFIX.length());
    }

    /** config.columns 中引用到的自定义字段 key。 */
    public static Set<String> referencedKeys(ViewConfig config) {
        if (config == null) {
            return Set.of();
        }
        Set<String> keys = new java.util.LinkedHashSet<>();
        if (config.columns() != null) {
            config.columns().stream().filter(TaskViewCustomField::isCf).map(TaskViewCustomField::key).forEach(keys::add);
        }
        if (config.filters() != null && config.filters().conditions() != null) {
            config.filters().conditions().stream().map(FilterCondition::field)
                    .filter(TaskViewCustomField::isCf).map(TaskViewCustomField::key).forEach(keys::add);
        }
        if (config.sort() != null) {
            config.sort().stream().map(SortSpec::field)
                    .filter(TaskViewCustomField::isCf).map(TaskViewCustomField::key).forEach(keys::add);
        }
        return keys;
    }

    /**
     * 拆分结果：
     * <ul>
     *   <li>{@code nativeConfig}：仅含可下推 SQL 的原生筛选/排序（cf 被剥离）；交给 translator。</li>
     *   <li>{@code memoryFilter}/{@code memoryLogic}：需在内存评估的筛选条件（cf；或 OR 含 cf 时为全部条件）。</li>
     *   <li>{@code memorySort}：需在内存评估的排序（含 cf 时为全部排序级，保证多级次序正确）。</li>
     * </ul>
     */
    public record Split(ViewConfig nativeConfig, List<FilterCondition> memoryFilter,
                        String memoryLogic, List<SortSpec> memorySort) {
    }

    public static Split split(ViewConfig config) {
        if (config == null) {
            return new Split(null, List.of(), "and", List.of());
        }
        FilterGroup fg = config.filters();
        List<FilterCondition> conds = fg == null || fg.conditions() == null ? List.of() : fg.conditions();
        String logic = fg == null ? "and" : fg.logic();
        boolean hasCfFilter = conds.stream().anyMatch(c -> isCf(c.field()));
        boolean or = "or".equalsIgnoreCase(logic);

        List<FilterCondition> sqlFilter;
        List<FilterCondition> memFilter;
        if (hasCfFilter && or) {
            // OR 语义下 cf 与原生混合不可拆分下推（下推会错误排除 OR 另一支命中行）→ 全部内存评估
            sqlFilter = List.of();
            memFilter = conds;
        } else {
            sqlFilter = conds.stream().filter(c -> !isCf(c.field())).toList();
            memFilter = conds.stream().filter(c -> isCf(c.field())).toList();
        }

        List<SortSpec> sort = config.sort() == null ? List.of() : config.sort();
        boolean hasCfSort = sort.stream().anyMatch(s -> isCf(s.field()));
        // 含 cf 排序时整体在内存排序（多级次序需统一处理）；否则下推 SQL
        List<SortSpec> sqlSort = hasCfSort ? List.of() : sort;
        List<SortSpec> memSort = hasCfSort ? sort : List.of();

        FilterGroup sqlGroup = sqlFilter.isEmpty() ? null : new FilterGroup(logic, sqlFilter);
        ViewConfig nativeConfig = new ViewConfig(config.groupBy(), sqlSort, config.expandLevel(),
                sqlGroup, config.columns());
        return new Split(nativeConfig, memFilter, or ? "or" : "and", memSort);
    }

    /** 内存匹配：conds 为空恒真。cfTypes：cf fieldKey → 类型码（FieldType.code），决定比较语义。 */
    public static boolean matches(TaskVO task, List<FilterCondition> conds, String logic,
                                  Map<String, String> cfTypes) {
        if (conds == null || conds.isEmpty()) {
            return true;
        }
        boolean or = "or".equalsIgnoreCase(logic);
        for (FilterCondition c : conds) {
            boolean hit = matchOne(task, c, cfTypes);
            if (or && hit) {
                return true;
            }
            if (!or && !hit) {
                return false;
            }
        }
        return !or;
    }

    private static boolean matchOne(TaskVO task, FilterCondition c, Map<String, String> cfTypes) {
        String hint = hintOf(c.field(), cfTypes);
        Object raw = valueOf(task, c.field());
        String op = c.op() == null ? "" : c.op();
        switch (op) {
            case "isNull":
                return isBlank(raw);
            case "notNull":
                return !isBlank(raw);
            default:
                break;
        }
        if ("in".equals(op)) {
            if (!(c.value() instanceof Collection<?> coll)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "in 算子值须为数组: " + c.field());
            }
            String left = canonical(raw, hint);
            return coll.stream().anyMatch(v -> matchScalar("eq", left, raw, canonical(v, hint), v, hint));
        }
        return matchScalar(op, canonical(raw, hint), raw, canonical(c.value(), hint), c.value(), hint);
    }

    private static boolean matchScalar(String op, String left, Object leftRaw,
                                       String right, Object rightRaw, String hint) {
        switch (op) {
            case "eq":
                return left != null && left.equals(right);
            case "ne":
                return left == null || !left.equals(right);
            case "like":
                return left != null && right != null
                        && left.toLowerCase().contains(right.toLowerCase());
            case "gt":
            case "ge":
            case "lt":
            case "le": {
                if (left == null || right == null) {
                    return false;
                }
                int cmp = compare(left, right, hint);
                return switch (op) {
                    case "gt" -> cmp > 0;
                    case "ge" -> cmp >= 0;
                    case "lt" -> cmp < 0;
                    default -> cmp <= 0;
                };
            }
            default:
                throw new BizException(ErrorCode.PARAM_ERROR, "非法算子: " + op);
        }
    }

    /** 多级排序比较器；null 值排末尾。 */
    public static Comparator<TaskVO> comparator(List<SortSpec> sorts, Map<String, String> cfTypes) {
        return (a, b) -> {
            for (SortSpec s : sorts) {
                String hint = hintOf(s.field(), cfTypes);
                String va = canonical(valueOf(a, s.field()), hint);
                String vb = canonical(valueOf(b, s.field()), hint);
                int cmp;
                if (va == null && vb == null) {
                    cmp = 0;
                } else if (va == null) {
                    cmp = 1; // null 末尾
                } else if (vb == null) {
                    cmp = -1;
                } else {
                    cmp = compare(va, vb, hint);
                }
                if (cmp != 0) {
                    return "desc".equalsIgnoreCase(s.dir()) ? -cmp : cmp;
                }
            }
            return 0;
        };
    }

    /** 字段取值：原生字段读 TaskVO，cf 字段读 customFields。 */
    private static Object valueOf(TaskVO t, String field) {
        if (isCf(field)) {
            Map<String, String> cf = t.customFields();
            return cf == null ? null : cf.get(key(field));
        }
        return switch (field) {
            case "title" -> t.title();
            case "description" -> t.description();
            case "status" -> t.status();
            case "stage" -> t.stage();
            case "assigneeId" -> t.assigneeId();
            case "parentId" -> t.parentId();
            case "priority" -> t.priority();
            case "isMilestone" -> t.isMilestone();
            case "startDate" -> t.startDate();
            case "dueDate" -> t.dueDate();
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "非法视图字段: " + field);
        };
    }

    /** 比较语义提示：number/decimal 数值，date 日期，其余按字符串。 */
    private static String hintOf(String field, Map<String, String> cfTypes) {
        if (isCf(field)) {
            String type = cfTypes == null ? null : cfTypes.get(key(field));
            return switch (type == null ? "" : type) {
                case "number" -> "decimal";
                case "date" -> "date";
                case "user" -> "decimal";
                default -> "string";
            };
        }
        return switch (field) {
            case "assigneeId", "parentId", "priority", "isMilestone" -> "decimal";
            case "startDate", "dueDate" -> "date";
            default -> "string";
        };
    }

    private static int compare(String a, String b, String hint) {
        if ("decimal".equals(hint)) {
            try {
                return new BigDecimal(a).compareTo(new BigDecimal(b));
            } catch (NumberFormatException ignored) {
                return a.compareTo(b);
            }
        }
        if ("date".equals(hint)) {
            try {
                return LocalDate.parse(a).compareTo(LocalDate.parse(b));
            } catch (Exception ignored) {
                return a.compareTo(b);
            }
        }
        return a.compareTo(b);
    }

    /** 归一为可比较字符串；数值去尾零，便于 "12.50" 与 "12.5" 等值比较。 */
    private static String canonical(Object v, String hint) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v);
        if (s.isBlank()) {
            return null;
        }
        if ("decimal".equals(hint)) {
            try {
                return new BigDecimal(s.trim()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return s;
            }
        }
        return s;
    }

    private static boolean isBlank(Object v) {
        return v == null || String.valueOf(v).isBlank();
    }

    /** 仅保留可作为展示列的 cf key（去重）。供服务层决定是否批量取值。 */
    public static List<String> columnKeys(ViewConfig config) {
        if (config == null || config.columns() == null) {
            return List.of();
        }
        return config.columns().stream().filter(TaskViewCustomField::isCf).map(TaskViewCustomField::key)
                .distinct().collect(Collectors.toCollection(ArrayList::new));
    }
}
