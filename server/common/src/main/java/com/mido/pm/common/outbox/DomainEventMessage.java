package com.mido.pm.common.outbox;

/**
 * 进程内领域事件消息。{@link DomainEventPublisher} 写 Outbox 的同时以 Spring 事件发布本对象，
 * 供同应用内的监听器（@TransactionalEventListener）即时响应、跨域解耦；
 * Outbox 行仍保留给将来 RabbitMQ 外部中继。
 *
 * @param eventType 事件名（取自 docs/domain-events.md）
 * @param payload   事件载荷（通常为 Map）
 * @param tenantId  租户
 */
public record DomainEventMessage(String eventType, Object payload, Long tenantId) {
}
