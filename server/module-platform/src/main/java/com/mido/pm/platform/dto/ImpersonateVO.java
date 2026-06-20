package com.mido.pm.platform.dto;

/**
 * 模拟登录返回：一个短时有效的租户令牌，运营人员据此以目标用户身份进入租户应用排障。
 *
 * @param token        租户访问令牌（短时）
 * @param tokenType    令牌类型（Bearer）
 * @param expiresIn    有效期（秒）
 * @param tenantId     目标租户 ID
 * @param tenantCode   目标租户编码
 * @param targetUserId 被模拟的租户用户 ID
 */
public record ImpersonateVO(String token, String tokenType, long expiresIn,
                            Long tenantId, String tenantCode, Long targetUserId) {
}
