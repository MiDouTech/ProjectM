package com.mido.pm.approval.dto;

/**
 * 审批 bizType 配置项（单一信息源，经 GET /approvals/biz-types 暴露给前端）。
 * 字段 value/label 与前端下拉形状一致，零映射。
 */
public record ApprovalBizTypeVO(String value, String label) {
}
