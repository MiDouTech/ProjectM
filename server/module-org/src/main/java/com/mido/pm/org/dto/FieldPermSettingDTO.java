package com.mido.pm.org.dto;

/**
 * 角色字段级权限配置项。access 取 view/edit（见 FieldAccess）。
 *
 * @param resource 资源：task/project
 * @param field    字段键，如 priority/status/assignee
 * @param access   访问级别：view 仅查看 / edit 可编辑
 */
public record FieldPermSettingDTO(String resource, String field, String access) {
}
