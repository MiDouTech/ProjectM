package com.mido.pm.org.dto;

/** 创建 API Key 的返回：明文 key 仅此一次返回，请妥善保存。 */
public record ApiKeyCreatedVO(Long id, String name, String apiKey, String keyPrefix) {
}
