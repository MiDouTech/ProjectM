package com.mido.pm.common.outbox;

import cn.hutool.json.JSONUtil;
import com.mido.pm.common.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 领域事件发布器（Outbox 模式）。
 * 在业务写操作的同一事务内调用 {@link #publish}：①落库为 pending（供将来 RabbitMQ 外部中继）；
 * ②发 Spring 进程内事件 {@link DomainEventMessage}（供同应用监听器即时跨域响应）。
 * 事件名须取自 docs/domain-events.md。
 */
@Service
public class DomainEventPublisher {

    private final DomainEventMapper domainEventMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(DomainEventMapper domainEventMapper,
                                ApplicationEventPublisher applicationEventPublisher) {
        this.domainEventMapper = domainEventMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 写入一条待投递领域事件并发进程内事件。须在调用方事务内执行，与业务变更同生共死。
     *
     * @param eventType 事件名（取自 docs/domain-events.md，不得自造）
     * @param payload   事件载荷对象，自动序列化为 JSON
     */
    public void publish(String eventType, Object payload) {
        Long tenantId = TenantContext.get();
        DomainEvent event = new DomainEvent();
        event.setTenantId(tenantId);
        event.setEventType(eventType);
        event.setPayload(payload == null ? null : JSONUtil.toJsonStr(payload));
        event.setStatus(DomainEvent.STATUS_PENDING);
        domainEventMapper.insert(event);

        applicationEventPublisher.publishEvent(new DomainEventMessage(eventType, payload, tenantId));
    }
}
