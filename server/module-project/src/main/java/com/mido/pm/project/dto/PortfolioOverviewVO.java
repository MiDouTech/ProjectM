package com.mido.pm.project.dto;

import java.util.List;
import java.util.Map;

/**
 * 项目集总览：项目集基本信息 + 当前用户可见的项目列表 + 按状态汇总计数。
 * projects 已按数据范围过滤（管理层看全量，部门成员看权限内），实现全局/局部视图隔离。
 *
 * @param item        项目集卡片摘要（id/name/owner 等）
 * @param projects    当前用户可见的项目摘要列表
 * @param statusCount 状态 → 数量（基于可见项目）
 */
public record PortfolioOverviewVO(
        PortfolioVO item,
        List<ProjectVO> projects,
        Map<String, Long> statusCount) {
}
