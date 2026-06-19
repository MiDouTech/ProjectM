package com.mido.pm.doc.dto;

/** 移动节点：改父节点与/或同级排序。parentId 缺省视为根(0)。 */
public record DocMoveDTO(
        Long parentId,
        Integer sortNo) {
}
