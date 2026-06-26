package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/** 项目安装组件项（保存时整列表先清后插，sort 取列表顺序）。 */
public record ProjectComponentSaveDTO(
        @NotBlank(message = "组件编码不能为空") String componentCode,
        String name,
        Integer sort) {
}
