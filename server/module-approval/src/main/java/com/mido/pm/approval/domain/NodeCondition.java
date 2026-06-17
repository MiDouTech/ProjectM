package com.mido.pm.approval.domain;

/**
 * 节点路由条件（conditional 分支）：按上下文字段比较决定节点是否启用。
 * field 取 amount/category/jobLevel；op 取 == / != / &gt; / &lt; / &gt;= / &lt;=。
 */
public record NodeCondition(String field, String op, String value) {
}
