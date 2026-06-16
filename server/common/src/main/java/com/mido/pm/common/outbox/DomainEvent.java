package com.mido.pm.common.outbox;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 领域事件（Outbox）实体，对应 docs/data-model.md 的 sys_domain_event。
 * 任何写操作须在同事务写入一条 pending 事件，事件名取自 docs/domain-events.md。
 */
@TableName("sys_domain_event")
public class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件初始状态：待投递 */
    public static final String STATUS_PENDING = "pending";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 事件名，形如 project.status.changed，取自 docs/domain-events.md，不得自造 */
    private String eventType;

    /** 事件载荷（JSON 字符串） */
    private String payload;

    /** 投递状态：pending / sent / failed */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
