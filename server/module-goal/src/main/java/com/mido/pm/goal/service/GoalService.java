package com.mido.pm.goal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.goal.domain.GoalProgress;
import com.mido.pm.goal.event.GoalEvents;
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

import com.mido.pm.goal.dto.AlignedGoalVO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 目标域服务：目标/KR 树（parent_id）CRUD + 量化进度（{@link GoalProgress}）+ 对齐网（弱关联，非父子）。
 * 写操作同事务发 Outbox 领域事件（goal.* 见 docs/domain-events.md）。
 * ★目标不挂执行树：对齐仅为 goal↔project/task 多对多弱引用（弱关联，非父子）。
 */
@Service
public class GoalService {

    private static final Set<String> TYPES = Set.of("objective", "kr");
    private static final Set<String> TARGET_TYPES = Set.of("project", "task");

    private final PmGoalMapper goalMapper;
    private final PmGoalAlignmentMapper alignmentMapper;
    private final DomainEventPublisher eventPublisher;
    private final GoalRollupService rollupService;
    private final ChangeService changeService;

    public GoalService(PmGoalMapper goalMapper, PmGoalAlignmentMapper alignmentMapper,
                       DomainEventPublisher eventPublisher, GoalRollupService rollupService,
                       ChangeService changeService) {
        this.goalMapper = goalMapper;
        this.alignmentMapper = alignmentMapper;
        this.eventPublisher = eventPublisher;
        this.rollupService = rollupService;
        this.changeService = changeService;
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
        g.setAutoRollup(dto.autoRollup() != null ? dto.autoRollup() : 0);
        goalMapper.insert(g);
        eventPublisher.publish(GoalEvents.CREATED, payload("goalId", g.getId(), "type", g.getType()));
        return g.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoalUpdateDTO dto) {
        PmGoal g = requireGoal(id);
        // 受控变更冻结：有进行中的变更单时，禁止直接编辑基线（防绕过审批流）；进度(updateMetric) 不受限
        if (changeService.hasPending(GoalChangeService.BIZ_TYPE, id)) {
            throw new BizException(ErrorCode.CONFLICT, "该目标有进行中的变更单，请走变更流程或先撤回后再编辑");
        }
        g.setTitle(dto.title());
        g.setOwnerId(dto.ownerId());
        g.setPeriod(dto.period());
        g.setMetricUnit(dto.metricUnit());
        g.setMetricStart(dto.metricStart());
        g.setMetricTarget(dto.metricTarget());
        g.setMetricCurrent(dto.metricCurrent());
        g.setProgress(GoalProgress.compute(dto.metricStart(), dto.metricTarget(), dto.metricCurrent()));
        g.setAutoRollup(dto.autoRollup() != null ? dto.autoRollup() : 0);
        goalMapper.updateById(g);
        eventPublisher.publish(GoalEvents.UPDATED, payload("goalId", id));
    }

    /** 量化指标行内编辑：仅更新当前值并重算进度。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateMetric(Long id, GoalMetricDTO dto) {
        PmGoal g = requireGoal(id);
        g.setMetricCurrent(dto.metricCurrent());
        g.setProgress(GoalProgress.compute(g.getMetricStart(), g.getMetricTarget(), dto.metricCurrent()));
        goalMapper.updateById(g);
        eventPublisher.publish(GoalEvents.PROGRESS_CHANGED,
                payload("goalId", id, "progress", g.getProgress()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireGoal(id);
        // 受控变更冻结：有进行中变更单时禁止删除（否则变更通过后无目标可回写，沦为空操作并误导审计）
        if (changeService.hasPending(GoalChangeService.BIZ_TYPE, id)) {
            throw new BizException(ErrorCode.CONFLICT, "该目标有进行中的变更单，请先完成或撤回变更后再删除");
        }
        goalMapper.deleteById(id);
        // 弱关联：删目标只删其对齐链，绝不级联删对齐的 project/task
        alignmentMapper.delete(Wrappers.<PmGoalAlignment>lambdaQuery()
                .eq(PmGoalAlignment::getGoalId, id));
        eventPublisher.publish(GoalEvents.DELETED, payload("goalId", id));
    }

    /** 对齐对方(project/task)被删时，仅清理对齐链（不动 goal）。由 GoalAlignmentCleanupListener 调用。 */
    @Transactional(rollbackFor = Exception.class)
    public void removeAlignmentsByTarget(String targetType, Long targetId) {
        int removed = alignmentMapper.delete(Wrappers.<PmGoalAlignment>lambdaQuery()
                .eq(PmGoalAlignment::getTargetType, targetType)
                .eq(PmGoalAlignment::getTargetId, targetId));
        if (removed > 0) {
            eventPublisher.publish(GoalEvents.UNALIGNED,
                    payload("targetType", targetType, "targetId", targetId, "count", removed));
        }
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
        a.setWeight(dto.weight() != null ? dto.weight() : java.math.BigDecimal.ONE);
        alignmentMapper.insert(a);
        eventPublisher.publish(GoalEvents.ALIGNED,
                payload("goalId", goalId, "targetType", dto.targetType(), "targetId", dto.targetId()));
        return a.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeAlignment(Long id) {
        PmGoalAlignment a = alignmentMapper.selectById(id);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "对齐不存在");
        }
        alignmentMapper.deleteById(id);
        eventPublisher.publish(GoalEvents.UNALIGNED,
                payload("goalId", a.getGoalId(), "targetType", a.getTargetType(),
                        "targetId", a.getTargetId(), "count", 1));
    }

    /** 调整对齐贡献权重（贡献度看板内编辑）；若 KR 开启自动汇总则即时重算其进度。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateAlignmentWeight(Long id, java.math.BigDecimal weight) {
        PmGoalAlignment a = alignmentMapper.selectById(id);
        if (a == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "对齐不存在");
        }
        if (weight == null || weight.signum() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "权重需为非负数");
        }
        a.setWeight(weight);
        alignmentMapper.updateById(a);
        rollupService.recomputeGoal(a.getGoalId());
    }

    /** 反向贡献度看板（委托汇总服务，只读）。 */
    public com.mido.pm.goal.dto.GoalContributionVO contribution(Long goalId) {
        return rollupService.contribution(goalId);
    }

    public List<AlignmentVO> listAlignments(Long goalId) {
        return alignmentMapper.selectList(Wrappers.<PmGoalAlignment>lambdaQuery()
                        .eq(PmGoalAlignment::getGoalId, goalId))
                .stream().map(this::toVO).toList();
    }

    /**
     * 反向查询：对齐到某对象（project/task）的全部目标（含对齐关系 id）。
     * 「项目工作台·目标」用：让目标为项目服务，直接看到/管理本项目对齐的 KR。
     */
    public List<AlignedGoalVO> listGoalsByTarget(String targetType, Long targetId) {
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法对齐类型: " + targetType);
        }
        List<PmGoalAlignment> aligns = alignmentMapper.selectList(
                Wrappers.<PmGoalAlignment>lambdaQuery()
                        .eq(PmGoalAlignment::getTargetType, targetType)
                        .eq(PmGoalAlignment::getTargetId, targetId));
        if (aligns.isEmpty()) {
            return List.of();
        }
        // goalId → 对齐 id（同一目标对同一对象唯一，addAlignment 已防重）
        Map<Long, Long> alignIdByGoal = new LinkedHashMap<>();
        for (PmGoalAlignment a : aligns) {
            alignIdByGoal.putIfAbsent(a.getGoalId(), a.getId());
        }
        return goalMapper.selectBatchIds(alignIdByGoal.keySet()).stream()
                .map(g -> new AlignedGoalVO(alignIdByGoal.get(g.getId()), toVO(g)))
                .toList();
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

    /** 事件载荷构造（保序键值对）。 */
    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private GoalVO toVO(PmGoal g) {
        return new GoalVO(g.getId(), g.getTitle(), g.getType(), g.getParentId(), g.getOwnerId(),
                g.getPeriod(), g.getMetricUnit(), g.getMetricStart(), g.getMetricTarget(),
                g.getMetricCurrent(), g.getProgress(), g.getAutoRollup());
    }

    private AlignmentVO toVO(PmGoalAlignment a) {
        return new AlignmentVO(a.getId(), a.getGoalId(), a.getTargetType(), a.getTargetId(), a.getWeight());
    }
}
