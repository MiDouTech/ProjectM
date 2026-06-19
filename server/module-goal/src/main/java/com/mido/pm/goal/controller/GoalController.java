package com.mido.pm.goal.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.goal.dto.AlignGraphVO;
import com.mido.pm.goal.dto.AlignedGoalVO;
import com.mido.pm.goal.dto.AlignmentCreateDTO;
import com.mido.pm.goal.dto.AlignmentVO;
import com.mido.pm.goal.dto.AlignmentWeightDTO;
import com.mido.pm.goal.dto.GoalContributionVO;
import com.mido.pm.goal.dto.GoalCreateDTO;
import com.mido.pm.goal.dto.GoalMetricDTO;
import com.mido.pm.goal.dto.GoalUpdateDTO;
import com.mido.pm.goal.dto.GoalVO;
import com.mido.pm.goal.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 目标/KR + 对齐网。目标树由前端按 parentId 组装；对齐为弱关联（非父子）。 */
@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody GoalCreateDTO dto) {
        return R.ok(goalService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody GoalUpdateDTO dto) {
        goalService.update(id, dto);
        return R.ok();
    }

    /** 量化指标行内编辑（仅当前值，进度自动重算）。 */
    @PutMapping("/{id}/metric")
    public R<Void> updateMetric(@PathVariable Long id, @Valid @RequestBody GoalMetricDTO dto) {
        goalService.updateMetric(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return R.ok();
    }

    @GetMapping
    public R<List<GoalVO>> list(@RequestParam(required = false) String period,
                                @RequestParam(required = false) Long ownerId) {
        return R.ok(goalService.list(period, ownerId));
    }

    @GetMapping("/align-graph")
    public R<AlignGraphVO> alignGraph() {
        return R.ok(goalService.alignGraph());
    }

    @GetMapping("/{id}")
    public R<GoalVO> get(@PathVariable Long id) {
        return R.ok(goalService.get(id));
    }

    @PostMapping("/{id}/alignments")
    public R<Long> addAlignment(@PathVariable Long id, @Valid @RequestBody AlignmentCreateDTO dto) {
        return R.ok(goalService.addAlignment(id, dto));
    }

    @GetMapping("/{id}/alignments")
    public R<List<AlignmentVO>> listAlignments(@PathVariable Long id) {
        return R.ok(goalService.listAlignments(id));
    }

    /** 反向贡献度看板：某 KR 各对齐项目的完成率/权重/贡献。 */
    @GetMapping("/{id}/contribution")
    public R<GoalContributionVO> contribution(@PathVariable Long id) {
        return R.ok(goalService.contribution(id));
    }

    /** 调整对齐贡献权重（贡献度看板内编辑）。 */
    @PutMapping("/alignments/{alignmentId}/weight")
    public R<Void> updateAlignmentWeight(@PathVariable Long alignmentId,
                                         @Valid @RequestBody AlignmentWeightDTO dto) {
        goalService.updateAlignmentWeight(alignmentId, dto.weight());
        return R.ok();
    }

    /** 反向查询：对齐到某对象（project/task）的目标（项目工作台·目标用）。 */
    @GetMapping("/by-target")
    public R<List<AlignedGoalVO>> listByTarget(@RequestParam String targetType,
                                               @RequestParam Long targetId) {
        return R.ok(goalService.listGoalsByTarget(targetType, targetId));
    }

    @DeleteMapping("/alignments/{alignmentId}")
    public R<Void> removeAlignment(@PathVariable Long alignmentId) {
        goalService.removeAlignment(alignmentId);
        return R.ok();
    }
}
