package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.CriticalPathCalculator;
import com.mido.pm.task.domain.TaskDependencyGraph;
import com.mido.pm.task.dto.CriticalPathVO;
import com.mido.pm.task.dto.DependencyCreateDTO;
import com.mido.pm.task.dto.DependencyVO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.entity.PmTaskDependency;
import com.mido.pm.task.mapper.PmTaskDependencyMapper;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务依赖服务：增删依赖（新增做循环依赖检测，成环则拒绝并提示环路径）+ 关键路径计算。
 * 依赖限定同项目内；pm_task_dependency 无对应领域事件（domain-events.md 未登记），按既有惯例不发事件。
 */
@Service
public class TaskDependencyService {

    /** 依赖类型默认值（FS：前置完成后后继开始） */
    private static final String DEFAULT_TYPE = "FS";

    private final PmTaskDependencyMapper depMapper;
    private final PmTaskMapper taskMapper;

    public TaskDependencyService(PmTaskDependencyMapper depMapper, PmTaskMapper taskMapper) {
        this.depMapper = depMapper;
        this.taskMapper = taskMapper;
    }

    /** 新增依赖：校验同项目、非自环、不重复，并做循环依赖检测；成环抛错并附环路径。 */
    @Transactional(rollbackFor = Exception.class)
    public Long add(DependencyCreateDTO dto) {
        PmTask pred = requireTask(dto.predecessorId());
        PmTask succ = requireTask(dto.successorId());
        if (!pred.getProjectId().equals(succ.getProjectId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "依赖只能在同一项目内建立");
        }
        if (exists(dto.predecessorId(), dto.successorId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "依赖已存在");
        }
        Map<Long, List<Long>> adjacency = adjacency(pred.getProjectId());
        List<Long> cycle = TaskDependencyGraph.detectCycle(
                adjacency, dto.predecessorId(), dto.successorId());
        if (!cycle.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "存在循环依赖：" + cycle.stream().map(String::valueOf).collect(Collectors.joining("→")));
        }
        PmTaskDependency d = new PmTaskDependency();
        d.setPredecessorId(dto.predecessorId());
        d.setSuccessorId(dto.successorId());
        d.setType(dto.type() == null || dto.type().isBlank() ? DEFAULT_TYPE : dto.type());
        depMapper.insert(d);
        return d.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (depMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "依赖不存在");
        }
        depMapper.deleteById(id);
    }

    /** 某项目的依赖清单。 */
    public List<DependencyVO> listByProject(Long projectId) {
        return projectDeps(projectId).stream()
                .map(d -> new DependencyVO(d.getId(), d.getPredecessorId(), d.getSuccessorId(), d.getType()))
                .toList();
    }

    /** 关键路径：基于项目内任务工期(start/due)与依赖，算关键任务集与项目工期。 */
    public CriticalPathVO criticalPath(Long projectId) {
        List<PmTask> tasks = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getProjectId, projectId));
        Map<Long, Long> durations = new HashMap<>();
        for (PmTask t : tasks) {
            durations.put(t.getId(), durationDays(t));
        }
        List<long[]> edges = projectDeps(projectId).stream()
                .map(d -> new long[]{d.getPredecessorId(), d.getSuccessorId()})
                .toList();
        CriticalPathCalculator.Result r = CriticalPathCalculator.compute(durations, edges);
        return new CriticalPathVO(List.copyOf(r.criticalTaskIds()), r.totalDurationDays());
    }

    // ===== 内部 =====

    /** 工期（天）= due - start；任一为空或 due<start 记 0。 */
    private long durationDays(PmTask t) {
        if (t.getStartDate() == null || t.getDueDate() == null || t.getDueDate().isBefore(t.getStartDate())) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(t.getStartDate(), t.getDueDate());
    }

    private Map<Long, List<Long>> adjacency(Long projectId) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (PmTaskDependency d : projectDeps(projectId)) {
            adj.computeIfAbsent(d.getPredecessorId(), k -> new ArrayList<>()).add(d.getSuccessorId());
        }
        return adj;
    }

    /** 项目内全部依赖（前置任务属于该项目即纳入；依赖限同项目）。 */
    private List<PmTaskDependency> projectDeps(Long projectId) {
        List<Long> taskIds = taskMapper.selectList(Wrappers.<PmTask>lambdaQuery()
                        .eq(PmTask::getProjectId, projectId))
                .stream().map(PmTask::getId).toList();
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return depMapper.selectList(Wrappers.<PmTaskDependency>lambdaQuery()
                .in(PmTaskDependency::getPredecessorId, taskIds));
    }

    private boolean exists(Long predId, Long succId) {
        return depMapper.selectCount(Wrappers.<PmTaskDependency>lambdaQuery()
                .eq(PmTaskDependency::getPredecessorId, predId)
                .eq(PmTaskDependency::getSuccessorId, succId)) > 0;
    }

    private PmTask requireTask(Long id) {
        PmTask t = taskMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在: " + id);
        }
        return t;
    }
}
