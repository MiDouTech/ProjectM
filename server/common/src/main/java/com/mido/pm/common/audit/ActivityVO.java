package com.mido.pm.common.audit;

import java.time.LocalDateTime;

/**
 * 活动流条目（对外）：谁(userId) 在何时(createTime) 做了什么(action) 改了什么(detail)。
 * detail 为已解析的结构（{from,to} 或 {changes:[...]}），由前端按 action 渲染可读文案。
 */
public record ActivityVO(
        Long id,
        Long userId,
        String action,
        Object detail,
        LocalDateTime createTime) {
}
