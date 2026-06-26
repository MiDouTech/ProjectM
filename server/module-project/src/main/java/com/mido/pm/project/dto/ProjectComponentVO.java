package com.mido.pm.project.dto;

/** 项目已安装组件条目。 */
public record ProjectComponentVO(Long id, String componentCode, String name, Integer sort) {
}
