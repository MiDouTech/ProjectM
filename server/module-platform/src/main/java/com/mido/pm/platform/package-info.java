/**
 * 平台域（SaaS 运营总后台）。
 *
 * <p>本域是<strong>跨租户的全局域</strong>，与租户内业务域（project/task/...）并列但不同维度：
 * 它管理「谁是租户、租户买了什么套餐、配额多少、何时到期、谁在运营」。因此本域所有表
 * <strong>不带 {@code tenant_id}</strong>，并登记在 {@code MidoTenantLineHandler} 的忽略表白名单中，
 * 不参与多租户行级隔离（这是对 CLAUDE.md 规则1「所有业务表必带 tenant_id」的正式架构例外）。</p>
 *
 * <p>平台账号体系独立于任何租户：运营人员登录走 {@code /api/v1/platform/auth/login}，
 * 使用独立密钥签发的 JWT（{@code PlatformTokenService}），与租户侧 SSO 物理隔离。</p>
 */
package com.mido.pm.platform;
