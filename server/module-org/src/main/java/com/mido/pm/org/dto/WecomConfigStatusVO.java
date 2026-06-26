package com.mido.pm.org.dto;

/**
 * 企微能力启用状态：供前端按钮/入口联动。
 * enabled = 开关开 且 corpId、对应 secret 均已配置（DB 配置或环境变量任一满足，由后端综合判断）。
 */
public record WecomConfigStatusVO(
        boolean contactsEnabled,
        boolean ssoEnabled,
        boolean msgEnabled) {
}
