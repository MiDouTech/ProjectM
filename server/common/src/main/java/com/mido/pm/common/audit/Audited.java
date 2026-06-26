package com.mido.pm.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式操作日志：标注在 Service 方法上，由 {@link AuditAspect} 在方法成功返回后自动写一条审计。
 *
 * <p>适用于不需要「变更前后」对比的粗粒度操作（创建/删除等）；需要 before/after diff 的敏感操作
 * （如角色权限/数据范围变更）仍应在业务事务内显式调用 {@link AuditLogService#record} 以捕获前值。</p>
 *
 * <p>targetId 解析约定（best-effort）：优先取方法返回值（若为 Long，对应 create 返回主键），
 * 否则取第一个 Long 类型入参（对应 update/delete 的 id 形参）。</p>
 *
 * <p>切面记录为 best-effort：审计写入异常只记录告警日志，不影响主业务。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** 功能模块，取自 {@link AuditActions} MODULE_*。 */
    String module();

    /** 动作码，取自 {@link AuditActions}。 */
    String action();

    /** 实体类型，取自 {@link AuditActions} TARGET_*。 */
    String target();
}
