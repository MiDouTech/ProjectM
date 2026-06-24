package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.field.dto.FieldDefVO;
import com.mido.pm.field.service.FieldDefService;
import com.mido.pm.field.service.FieldValueService;
import com.mido.pm.task.domain.TaskViewCustomField;
import com.mido.pm.task.domain.ViewQueryTranslator;
import com.mido.pm.task.dto.GroupedTasksVO;
import com.mido.pm.task.dto.GroupedTasksVO.TaskGroup;
import com.mido.pm.task.dto.TaskVO;
import com.mido.pm.task.dto.ViewQueryRequest;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.view.dto.ViewConfig;
import com.mido.pm.view.service.ViewService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按视图配置查询任务：解析 config（viewId 或内联）→ translator 转原生条件 → 查 pm_task → 内存分组。
 * 仅查本项目任务（project_id 限定）；tenant 由拦截器注入。
 *
 * <p>自定义字段（{@code cf:<fieldKey>}）经 {@link TaskViewCustomField} 处理：原生筛选/排序下推 SQL，
 * cf 列在结果集（≤{@link #MAX_RESULT}）上内存取值/筛选/排序（A 内存方案）。</p>
 */
@Service
public class TaskViewQueryService {

    /** 视图查询结果硬上限（大项目防全表；超限提示收窄筛选）。cf 筛选发生在此上限之后。 */
    private static final int MAX_RESULT = 500;

    /** 视图查询的实体类型（自定义字段作用域）。 */
    private static final String ENTITY_TYPE = "task";

    private final PmTaskMapper taskMapper;
    private final ViewService viewService;
    private final FieldValueService fieldValueService;
    private final FieldDefService fieldDefService;

    public TaskViewQueryService(PmTaskMapper taskMapper, ViewService viewService,
                                FieldValueService fieldValueService, FieldDefService fieldDefService) {
        this.taskMapper = taskMapper;
        this.viewService = viewService;
        this.fieldValueService = fieldValueService;
        this.fieldDefService = fieldDefService;
    }

    public GroupedTasksVO query(ViewQueryRequest req) {
        ViewConfig config = req.config();
        if (config == null && req.viewId() != null) {
            config = viewService.getConfig(req.viewId());
        }
        TaskViewCustomField.Split split = TaskViewCustomField.split(config);

        // 原生部分下推 SQL；cf 部分留待内存
        QueryWrapper<PmTask> qw = ViewQueryTranslator.build(split.nativeConfig());
        qw.eq("project_id", req.projectId());
        qw.last("limit " + MAX_RESULT);
        List<TaskVO> tasks = taskMapper.selectList(qw).stream().map(this::toVO)
                .collect(Collectors.toCollection(ArrayList::new));

        // cf：若视图引用到自定义字段，批量取值并挂到结果，再内存筛选/排序
        boolean usesCf = !TaskViewCustomField.referencedKeys(config).isEmpty();
        if (usesCf && !tasks.isEmpty()) {
            Map<Long, Map<String, String>> valuesByTask = fieldValueService.valuesForEntities(
                    ENTITY_TYPE, tasks.stream().map(TaskVO::id).toList());
            tasks.replaceAll(t -> t.withCustomFields(valuesByTask.getOrDefault(t.id(), Map.of())));
            Map<String, String> cfTypes = cfTypes();
            if (!split.memoryFilter().isEmpty()) {
                tasks = tasks.stream()
                        .filter(t -> TaskViewCustomField.matches(t, split.memoryFilter(), split.memoryLogic(), cfTypes))
                        .collect(Collectors.toCollection(ArrayList::new));
            }
            if (!split.memorySort().isEmpty()) {
                tasks.sort(TaskViewCustomField.comparator(split.memorySort(), cfTypes));
            }
        }

        String groupBy = config == null ? null : config.groupBy();
        Integer expandLevel = config == null ? null : config.expandLevel();
        List<String> columns = config == null ? List.of() : config.columns();
        return new GroupedTasksVO(groupBy, expandLevel, columns, group(tasks, groupBy));
    }

    /** 启用的任务级自定义字段：fieldKey → 类型码（内存比较语义用）。 */
    private Map<String, String> cfTypes() {
        return fieldDefService.list(ENTITY_TYPE, true).stream()
                .collect(Collectors.toMap(FieldDefVO::fieldKey, FieldDefVO::type, (a, b) -> a));
    }

    private List<TaskGroup> group(List<TaskVO> tasks, String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return List.of(new TaskGroup(null, tasks));
        }
        Map<Object, List<TaskVO>> buckets = new LinkedHashMap<>();
        for (TaskVO t : tasks) {
            buckets.computeIfAbsent(groupValue(t, groupBy), k -> new ArrayList<>()).add(t);
        }
        List<TaskGroup> groups = new ArrayList<>();
        buckets.forEach((k, v) -> groups.add(new TaskGroup(k, v)));
        return groups;
    }

    private Object groupValue(TaskVO t, String groupBy) {
        return switch (groupBy) {
            case "status" -> t.status();
            case "assigneeId" -> t.assigneeId();
            case "priority" -> t.priority();
            case "stage" -> t.stage();
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "不支持的分组字段: " + groupBy);
        };
    }

    private TaskVO toVO(PmTask t) {
        return new TaskVO(t.getId(), t.getProjectId(), t.getParentId(), t.getTitle(), t.getDescription(),
                t.getAssigneeId(), t.getStatus(), t.getPriority(), t.getStage(),
                t.getStartDate(), t.getDueDate(), t.getIsMilestone(), t.getCreateTime());
    }
}
