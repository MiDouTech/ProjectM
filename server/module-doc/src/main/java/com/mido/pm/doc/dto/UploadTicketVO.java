package com.mido.pm.doc.dto;

/**
 * 上传凭证：attachmentId（登记记录）+ 限时预签名 PUT URL（客户端直传）。
 * oss_key 不作为字段返回；仅内含于 uploadUrl 中供本次 PUT 使用。
 */
public record UploadTicketVO(Long attachmentId, String uploadUrl, long expireSeconds) {
}
