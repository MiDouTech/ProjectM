package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段级权限守卫：业务写操作在改动受控字段前调用，强制「仅查看」字段不可被编辑。
 * 这是字段级权限的安全边界（前端只读渲染只是 UX，真正拦截在此）。
 * 数据来自当前请求的 {@link CurrentUser#getViewOnlyFields()}（登录时按多角色合并取最宽算好）。
 */
@Component
public class FieldPermGuard {

    /** 若 (resource, field) 对当前用户为只读，则抛 403。无登录上下文时放行（如系统/定时任务）。 */
    public void assertEditable(String resource, String field) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return;
        }
        if (!user.isFieldEditable(resource, field)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权编辑字段: " + field);
        }
    }

    /** 当前用户在某资源下的只读字段键集合（仅 field 名，不含 resource 前缀），供前端只读渲染。 */
    public Set<String> viewOnlyFields(String resource) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return Set.of();
        }
        String prefix = resource + ".";
        return user.getViewOnlyFields().stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> k.substring(prefix.length()))
                .collect(Collectors.toSet());
    }
}
