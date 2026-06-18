package com.mido.pm.goal.dto;

/**
 * 对齐到某目标对象（project/task）的目标视图：在 {@link GoalVO} 基础上附带对齐关系 id，
 * 便于前端在「项目工作台·目标」中展示并支持解除对齐。
 */
public record AlignedGoalVO(Long alignmentId, GoalVO goal) {
}
