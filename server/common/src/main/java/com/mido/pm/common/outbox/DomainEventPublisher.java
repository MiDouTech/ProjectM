package com.mido.pm.common.outbox;

import cn.hutool.json.JSONUtil;
import com.mido.pm.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

/**
 * 领域事件发布器（Outbox 模式）。
 * 在业务写操作的同一事务内调用 {@link #publish}，将事件落库为 pending；
 * 由独立的中继任务（后续）扫描并投递到 RabbitMQ。事件名须取自 docs/domain-events.md。
 */
@Service
public class DomainEventPublisher {

    private final DomainEventMapper domainEventMapper;

    public DomainEventPublisher(DomainEventMapper domainEventMapper) {
        this.domainEventMapper = domainEventMapper;
    }

    /**
     * 写入一条待投递领域事件。须在调用方事务内执行，与业务变更同生共死。
     *
     * @param eventType 事件名（取自 docs/domain-events.md，不得自造）
     * @param payload   事件载荷对象，自动序列化为 JSON
     */
    public void publish(String eventType, Object payload) {
        DomainEvent event = new DomainEvent();
        event.setTenantId(TenantContext.get());
        event.setEventType(eventType);
        event.setPayload(payload == null ? null : JSONUtil.toJsonStr(payload));
        event.setStatus(DomainEvent.STATUS_PENDING);
        domainEventMapper.insert(event);
    }
}
