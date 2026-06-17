package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
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

/**
 * 按视图配置查询任务：解析 config（viewId 或内联）→ translator 转条件 → 查 pm_task → 内存分组。
 * 仅查本项目任务（project_id 限定）；tenant 由拦截器注入。
 */
@Service
public class TaskViewQueryService {

    private final PmTaskMapper taskMapper;
    private final ViewService viewService;

    public TaskViewQueryService(PmTaskMapper taskMapper, ViewService viewService) {
        this.taskMapper = taskMapper;
        this.viewService = viewService;
    }

    public GroupedTasksVO query(ViewQueryRequest req) {
        ViewConfig config = req.config();
        if (config == null && req.viewId() != null) {
            config = viewService.getConfig(req.viewId());
        }
        QueryWrapper<PmTask> qw = ViewQueryTranslator.build(config);
        qw.eq("project_id", req.projectId());
        List<TaskVO> tasks = taskMapper.selectList(qw).stream().map(this::toVO).toList();

        String groupBy = config == null ? null : config.groupBy();
        Integer expandLevel = config == null ? null : config.expandLevel();
        List<String> columns = config == null ? List.of() : config.columns();
        return new GroupedTasksVO(groupBy, expandLevel, columns, group(tasks, groupBy));
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
