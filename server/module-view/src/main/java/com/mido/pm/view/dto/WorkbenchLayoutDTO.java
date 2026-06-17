package com.mido.pm.view.dto;

import java.util.List;

/**
 * 工作台布局：有序的卡片 id 列表（顺序即展示/排序结果）。cards 为 null 表示未保存过（前端用默认布局）。
 */
public record WorkbenchLayoutDTO(List<String> cards) {
}
