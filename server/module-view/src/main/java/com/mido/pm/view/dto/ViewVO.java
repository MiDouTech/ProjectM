package com.mido.pm.view.dto;

/** 视图视图对象。config 已解析为结构化 {@link ViewConfig}。 */
public record ViewVO(
        Long id,
        String name,
        String scope,
        String type,
        Long projectId,
        Long ownerId,
        ViewConfig config) {
}
