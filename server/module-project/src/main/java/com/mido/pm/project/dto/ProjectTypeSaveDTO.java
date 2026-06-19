package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 新建/更新项目类型。code 为租户内唯一程序引用；更新时 code 不可改（以路径 id 为准）。
 *
 * @param code           程序引用码（如 S/I/O_NORMAL）
 * @param name           显示名
 * @param parentCode     上级类型码（报表汇总，可空）
 * @param color          design-system 颜色 token 名（可空）
 * @param icon           图标名（可空）
 * @param sort           排序（小在前，可空）
 * @param minJobLevel    立项 Leader 最低职级门槛（如 L3），空=不限
 * @param requiresNpss   默认是否走 NPSS：1 是 / 0 否（空按 1）
 * @param defaultFlowId  绑定默认审批流 id（可空，P1 接通）
 * @param stakeholderTpl 默认干系人权重模板 JSON（可空）
 * @param description    描述（可空）
 */
public record ProjectTypeSaveDTO(
        @NotBlank(message = "类型码不能为空") String code,
        @NotBlank(message = "类型名不能为空") String name,
        String parentCode,
        String color,
        String icon,
        Integer sort,
        String minJobLevel,
        Integer requiresNpss,
        Long defaultFlowId,
        String stakeholderTpl,
        String description) {
}
