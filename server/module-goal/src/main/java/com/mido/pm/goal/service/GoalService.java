package com.mido.pm.goal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.goal.domain.GoalProgress;
import com.mido.pm.goal.dto.AlignGraphVO;
import com.mido.pm.goal.dto.AlignmentCreateDTO;
import com.mido.pm.goal.dto.AlignmentVO;
import com.mido.pm.goal.dto.GoalCreateDTO;
import com.mido.pm.goal.dto.GoalMetricDTO;
import com.mido.pm.goal.dto.GoalUpdateDTO;
import com.mido.pm.goal.dto.GoalVO;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.entity.PmGoalAlignment;
import com.mido.pm.goal.mapper.PmGoalAlignmentMapper;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 目标域服务：目标/KR 树（parent_id）CRUD + 量化进度（{@link GoalProgress}）+ 对齐网（弱关联，非父子）。
 * pm_goal/pm_goal_alignment 无对应领域事件（domain-events.md 未登记），按既有惯例写操作不发事件、不自造。
 * ★目标不挂执行树：对齐仅为 goal↔project/task 多对多弱引用；项目进度→KR 为只读展示，P1 不自动反写。
 */
@Service
public class GoalService {

    private static final Set<String> TYPES = Set.of("objective", "kr");
    private static final Set<String> TARGET_TYPES = Set.of("project", "task");

    private final PmGoalMapper goalMapper;
    private final PmGoalAlignmentMapper alignmentMapper;

    public GoalService(PmGoalMapper goalMapper, PmGoalAlignmentMapper alignmentMapper) {
        this.goalMapper = goalMapper;
        this.alignmentMapper = alignmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(GoalCreateDTO dto) {
        if (!TYPES.contains(dto.type())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法目标类型: " + dto.type());
        }
        PmGoal g = new PmGoal();
        g.setTitle(dto.title());
        g.setType(dto.type());
        g.setParentId(dto.parentId() == null ? 0L : dto.parentId());
        g.setOwnerId(dto.ownerId() == null ? UserContext.currentUserId() : dto.ownerId());
        g.setPeriod(dto.period());
        g.setMetricUnit(dto.metricUnit());
        g.setMetricStart(dto.metricStart());
        g.setMetricTarget(dto.metricTarget());
        g.setMetricCurrent(dto.metricCurrent());
        g.setProgress(GoalProgress.compute(dto.metricStart(), dto.metricTarget(), dto.metricCurrent()));
        goalMapper.insert(g);
        return g.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoalUpdateDTO dto) {
        PmGoal g = requireGoal(id);
        g.setTitle(dto.title());
        g.setOwnerId(dto.ownerId());
        g.setPeriod(dto.period());
        g.setMetricUnit(dto.metricUnit());
        g.setMetricStart(dto.metricStart());
        g.setMetricTarget(dto.metricTarget());
        g.setMetricCurrent(dto.metricCurrent());
        g.setProgress(GoalProgress.compute(dto.metricStart(), dto.metricTarget(), dto.metricCurrent()));
        goalMapper.updateById(g);
    }

    /** 量化指标行内编辑：仅更新当前值并重算进度。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateMetric(Long id, GoalMetricDTO dto) {
        PmGoal g = requireGoal(id);
        g.setMetricCurrent(dto.metricCurrent());
        g.setProgress(GoalProgress.compute(g.getMetricStart(), g.getMetricTarget(), dto.metricCurrent()));
        goalMapper.updateById(g);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireGoal(id);
        goalMapper.deleteById(id);
    }

    /** 目标列表（可按 period/owner 过滤）；前端按 parentId 组装目标树。 */
    public List<GoalVO> list(String period, Long ownerId) {
        return goalMapper.selectList(Wrappers.<PmGoal>lambdaQuery()
                        .eq(period != null && !period.isBlank(), PmGoal::getPeriod, period)
                        .eq(ownerId != null, PmGoal::getOwnerId, ownerId)
                        .orderByAsc(PmGoal::getParentId).orderByDesc(PmGoal::getId))
                .stream().map(this::toVO).toList();
    }

    public GoalVO get(Long id) {
        return toVO(requireGoal(id));
    }

    // ===== 对齐网 =====

    @Transactional(rollbackFor = Exception.class)
    public Long addAlignment(Long goalId, AlignmentCreateDTO dto) {
        requireGoal(goalId);
        if (!TARGET_TYPES.contains(dto.targetType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法对齐类型: " + dto.targetType());
        }
        boolean dup = alignmentMapper.selectCount(Wrappers.<PmGoalAlignment>lambdaQuery()
                .eq(PmGoalAlignment::getGoalId, goalId)
                .eq(PmGoalAlignment::getTargetType, dto.targetType())
                .eq(PmGoalAlignment::getTargetId, dto.targetId())) > 0;
        if (dup) {
            throw new BizException(ErrorCode.PARAM_ERROR, "对齐已存在");
        }
        PmGoalAlignment a = new PmGoalAlignment();
        a.setGoalId(goalId);
        a.setTargetType(dto.targetType());
        a.setTargetId(dto.targetId());
        alignmentMapper.insert(a);
        return a.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeAlignment(Long id) {
        if (alignmentMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "对齐不存在");
        }
        alignmentMapper.deleteById(id);
    }

    public List<AlignmentVO> listAlignments(Long goalId) {
        return alignmentMapper.selectList(Wrappers.<PmGoalAlignment>lambdaQuery()
                        .eq(PmGoalAlignment::getGoalId, goalId))
                .stream().map(this::toVO).toList();
    }

    /** 对齐网全量（目标树 + 对齐边），供 G6 渲染。 */
    public AlignGraphVO alignGraph() {
        List<GoalVO> goals = goalMapper.selectList(Wrappers.<PmGoal>lambdaQuery()
                .orderByAsc(PmGoal::getParentId)).stream().map(this::toVO).toList();
        List<AlignmentVO> alignments = alignmentMapper.selectList(Wrappers.<PmGoalAlignment>lambdaQuery())
                .stream().map(this::toVO).toList();
        return new AlignGraphVO(goals, alignments);
    }

    // ===== 内部 =====

    private PmGoal requireGoal(Long id) {
        PmGoal g = goalMapper.selectById(id);
        if (g == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "目标不存在");
        }
        return g;
    }

    private GoalVO toVO(PmGoal g) {
        return new GoalVO(g.getId(), g.getTitle(), g.getType(), g.getParentId(), g.getOwnerId(),
                g.getPeriod(), g.getMetricUnit(), g.getMetricStart(), g.getMetricTarget(),
                g.getMetricCurrent(), g.getProgress());
    }

    private AlignmentVO toVO(PmGoalAlignment a) {
        return new AlignmentVO(a.getId(), a.getGoalId(), a.getTargetType(), a.getTargetId());
    }
}
