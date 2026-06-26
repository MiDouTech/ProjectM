package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.Audited;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.RelationKind;
import com.mido.pm.task.dto.RelationCreateDTO;
import com.mido.pm.task.dto.TaskRelationVO;
import com.mido.pm.task.entity.PmRelation;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmRelationMapper;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工作项关联（实例）服务：在任务之间建立 related(相关)/derived(派生) 关联，支撑追溯链。
 * 列表返回双向（本任务为源的 outgoing + 为目标的 incoming），含另一端任务的标题/状态。
 */
@Service
public class RelationService {

    private final PmRelationMapper relationMapper;
    private final PmTaskMapper taskMapper;

    public RelationService(PmRelationMapper relationMapper, PmTaskMapper taskMapper) {
        this.relationMapper = relationMapper;
        this.taskMapper = taskMapper;
    }

    @Audited(module = AuditActions.MODULE_TASK, action = AuditActions.CREATED, target = AuditActions.TARGET_RELATION)
    public Long link(Long sourceTaskId, RelationCreateDTO dto) {
        if (!RelationKind.isValid(dto.relationKind())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法关系类型: " + dto.relationKind());
        }
        if (Objects.equals(sourceTaskId, dto.targetTaskId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能关联到自身");
        }
        requireTask(sourceTaskId);
        requireTask(dto.targetTaskId());
        Long dup = relationMapper.selectCount(Wrappers.<PmRelation>lambdaQuery()
                .eq(PmRelation::getSourceTaskId, sourceTaskId)
                .eq(PmRelation::getTargetTaskId, dto.targetTaskId())
                .eq(PmRelation::getRelationKind, dto.relationKind()));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "关联已存在");
        }
        PmRelation r = new PmRelation();
        r.setSourceTaskId(sourceTaskId);
        r.setTargetTaskId(dto.targetTaskId());
        r.setRelationKind(dto.relationKind());
        relationMapper.insert(r);
        return r.getId();
    }

    @Audited(module = AuditActions.MODULE_TASK, action = AuditActions.DELETED, target = AuditActions.TARGET_RELATION)
    public void unlink(Long taskId, Long relationId) {
        PmRelation r = relationMapper.selectById(relationId);
        if (r == null || (!Objects.equals(r.getSourceTaskId(), taskId)
                && !Objects.equals(r.getTargetTaskId(), taskId))) {
            throw new BizException(ErrorCode.NOT_FOUND, "关联不存在");
        }
        relationMapper.deleteById(relationId);
    }

    /** 某任务的全部关联（双向）：本任务为源→outgoing，为目标→incoming。 */
    public List<TaskRelationVO> listForTask(Long taskId) {
        List<PmRelation> rels = relationMapper.selectList(Wrappers.<PmRelation>lambdaQuery()
                .eq(PmRelation::getSourceTaskId, taskId)
                .or().eq(PmRelation::getTargetTaskId, taskId));
        if (rels.isEmpty()) {
            return List.of();
        }
        Set<Long> otherIds = new LinkedHashSet<>();
        for (PmRelation r : rels) {
            otherIds.add(Objects.equals(r.getSourceTaskId(), taskId) ? r.getTargetTaskId() : r.getSourceTaskId());
        }
        Map<Long, PmTask> tasks = new HashMap<>();
        for (PmTask t : taskMapper.selectBatchIds(otherIds)) {
            tasks.put(t.getId(), t);
        }
        List<TaskRelationVO> out = new ArrayList<>();
        for (PmRelation r : rels) {
            boolean outgoing = Objects.equals(r.getSourceTaskId(), taskId);
            Long otherId = outgoing ? r.getTargetTaskId() : r.getSourceTaskId();
            PmTask other = tasks.get(otherId);
            out.add(new TaskRelationVO(r.getId(), r.getRelationKind(), outgoing ? "outgoing" : "incoming",
                    otherId, other == null ? null : other.getTitle(), other == null ? null : other.getStatus()));
        }
        return out;
    }

    private void requireTask(Long taskId) {
        if (taskMapper.selectById(taskId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在: " + taskId);
        }
    }
}
