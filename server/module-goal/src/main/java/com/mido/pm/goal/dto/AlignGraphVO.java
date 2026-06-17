package com.mido.pm.goal.dto;

import java.util.List;

/** 对齐网图数据：目标(含 parentId 树) + 对齐边，供前端 G6 GoalAlignTree 渲染。 */
public record AlignGraphVO(List<GoalVO> goals, List<AlignmentVO> alignments) {
}
