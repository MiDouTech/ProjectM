package com.mido.pm.platform.dto;

import java.math.BigDecimal;

/**
 * 收入汇总。
 *
 * @param totalPayment 收款合计
 * @param totalRefund  退款合计
 * @param net          净收入（收款-退款）
 * @param count        记录数
 */
public record RevenueSummaryVO(BigDecimal totalPayment, BigDecimal totalRefund, BigDecimal net, long count) {
}
