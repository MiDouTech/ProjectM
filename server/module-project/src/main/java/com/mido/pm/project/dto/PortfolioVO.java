package com.mido.pm.project.dto;

/** 项目集列表项。projectCount 为关联项目总数（未按数据范围过滤的登记数）。 */
public record PortfolioVO(Long id, String name, String description, Long ownerId, String status, long projectCount) {
}
