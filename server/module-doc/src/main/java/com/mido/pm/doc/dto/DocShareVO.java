package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/** 分享外链信息。token 拼成前端可访问的公开链接。 */
public record DocShareVO(Long id, Long docId, String token, LocalDateTime expireTime, Integer enabled) {
}
