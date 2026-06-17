package com.mido.pm.approval.domain;

import java.util.Map;

/**
 * 审批上下文：条件路由与节点 guard 据此判断。标准字段 amount/category/jobLevel。
 */
public final class ApprovalContext {

    private final Map<String, Object> attrs;

    public ApprovalContext(Map<String, Object> attrs) {
        this.attrs = attrs == null ? Map.of() : attrs;
    }

    public Object get(String field) {
        return attrs.get(field);
    }

    public String category() {
        Object v = attrs.get("category");
        return v == null ? null : v.toString();
    }

    public String jobLevel() {
        Object v = attrs.get("jobLevel");
        return v == null ? null : v.toString();
    }
}
