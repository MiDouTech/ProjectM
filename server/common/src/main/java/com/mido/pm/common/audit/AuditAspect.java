package com.mido.pm.common.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * {@link Audited} 注解切面：方法成功返回后，best-effort 写一条操作日志。
 * 审计失败只记录告警，绝不影响主业务流程（与显式 record() 的「同事务强一致」分工互补）。
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = pjp.proceed();
        try {
            Long targetId = resolveTargetId(result, pjp.getArgs());
            auditLogService.record(audited.module(), audited.target(), targetId, audited.action(), null);
        } catch (Exception e) {
            // best-effort：审计写入失败不阻断业务
            Method m = ((MethodSignature) pjp.getSignature()).getMethod();
            log.warn("操作日志记录失败（{}），已忽略: {}", m.getName(), e.getMessage());
        }
        return result;
    }

    /** 优先取返回值（create 返回主键），否则取第一个 Long 入参（update/delete 的 id）。 */
    private Long resolveTargetId(Object result, Object[] args) {
        if (result instanceof Long id) {
            return id;
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long id) {
                    return id;
                }
            }
        }
        return null;
    }
}
