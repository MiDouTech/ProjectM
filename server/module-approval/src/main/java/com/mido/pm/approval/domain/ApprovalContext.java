package com.mido.pm.approval.domain;

import java.util.Map;

/**
 * 审批上下文：条件路由与节点 guard 据此判断。标准字段 amount/category/jobLevel/minJobLevel。
 * minJobLevel 为项目类型配置的最低职级门槛（取代原按 category 硬编码），供 JOB_LEVEL guard 使用。
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

    /** 最低职级门槛（项目类型 min_job_level），空=不限。 */
    public String minJobLevel() {
        Object v = attrs.get("minJobLevel");
        return v == null ? null : v.toString();
    }
}
