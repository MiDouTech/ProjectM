package com.mido.pm.project.dto;

/** 项目角色对外视图。 */
public record ProjectRoleVO(Long id, String code, String name, Integer builtin, Integer sort, String status) {
}
