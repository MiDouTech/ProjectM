package com.mido.pm.provider.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 企微应用消息 Provider（预演实现）。
 *
 * <p>注册为 Bean 并参与 {@code NotificationListener} 的多通道扇出；具体哪些事件走企微由
 * {@link MessageRouting} 决定（本类只负责"企微通道的投递"）。</p>
 *
 * <p><b>开关与凭证</b>：是否真发由 {@code mido.wecom.message.enabled} 控制，<b>默认关</b>，
 * 关闭时走 Mock（仅日志打印，不外呼），便于沙箱/无凭证环境预演链路。corpId/secret/agentId
 * 一律走配置（环境变量注入），不硬编码。真实企微 API 接入留待 P2（{@link #dispatch}）。</p>
 */
@Component
public class WecomMessageProvider implements MessageProvider {

    private static final Logger log = LoggerFactory.getLogger(WecomMessageProvider.class);

    private final boolean enabled;
    private final String corpId;
    private final String secret;
    private final String agentId;

    public WecomMessageProvider(
            @Value("${mido.wecom.message.enabled:false}") boolean enabled,
            @Value("${mido.wecom.corp-id:}") String corpId,
            @Value("${mido.wecom.message.secret:}") String secret,
            @Value("${mido.wecom.message.agent-id:}") String agentId) {
        this.enabled = enabled;
        this.corpId = corpId;
        this.secret = secret;
        this.agentId = agentId;
    }

    @Override
    public String channel() {
        return CHANNEL_WECOM;
    }

    @Override
    public void send(Long userId, String title, String content) {
        if (userId == null) {
            return;
        }
        if (!enabled || corpId == null || corpId.isBlank()) {
            // 预演：默认关 / 缺凭证时不外呼，仅打印 Mock 推送，链路照常贯通。
            log.info("[Mock企微] 预演推送 agentId={} -> userId={} | {} | {}",
                    maskedAgentId(), userId, title, content);
            return;
        }
        dispatch(userId, title, content);
    }

    /**
     * 真实企微应用消息投递（P2 接入企微 message/send API）。
     * 当前为沙箱占位：仅记录将要外呼的事实，不在仓库内置任何凭证或真实外呼逻辑。
     */
    protected void dispatch(Long userId, String title, String content) {
        log.info("[企微·真发] (沙箱占位) corpId={} agentId={} -> userId={} | {} | {}",
                corpId, maskedAgentId(), userId, title, content);
    }

    /** agentId 仅日志展示，做轻度脱敏，避免明文落日志。 */
    private String maskedAgentId() {
        if (agentId == null || agentId.isBlank()) {
            return "<unset>";
        }
        return agentId.length() <= 2 ? "**" : agentId.charAt(0) + "***" + agentId.charAt(agentId.length() - 1);
    }
}
