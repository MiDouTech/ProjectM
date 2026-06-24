/**
 * 自定义字段域：字段定义(pm_field_def) + 字段值(pm_field_value, EAV)。
 *
 * <p>与协作域同形：以 (entity_type, entity_id) 跨任务/项目挂载，不反向依赖业务域；
 * 任务/项目详情经 {@code /api/v1/field-values} 读写字段值，无需 Java 跨模块依赖。</p>
 *
 * <p>模块内分层（CLAUDE.md §4）：controller / service / domain / mapper / entity / dto。
 * 字段值变更并入业务实体活动流（AuditLog），不新增 Outbox 事件名（遵循 docs/domain-events.md）。</p>
 */
package com.mido.pm.field;
