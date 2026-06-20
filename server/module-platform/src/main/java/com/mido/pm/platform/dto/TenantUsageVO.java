package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/**
 * 租户某资源的用量视图。
 *
 * @param resource    资源标识
 * @param used        当前用量
 * @param limit       有效上限（-1=不限）
 * @param exceeded    是否已超限
 * @param snapshotTime 最近快照时间
 */
public record TenantUsageVO(String resource, long used, long limit, boolean exceeded,
                            LocalDateTime snapshotTime) {
}
