package com.mido.pm.provider.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * 对象存储能力（S3 兼容：MinIO/OSS）。外部能力统一走 provider 接口（CLAUDE.md 规则 5），
 * 业务层只持有存储 key，绝不直连 SDK，也不向前端外泄 key——下载一律走限时预签名 URL。
 */
public interface StorageProvider {

    /**
     * 上传对象。
     *
     * @param key         存储键（由调用方生成，含租户/实体维度）
     * @param data        数据流
     * @param size        字节数
     * @param contentType MIME 类型，可空
     */
    void put(String key, InputStream data, long size, String contentType);

    /**
     * 生成限时预签名下载 URL（HTTP GET）。URL 自带签名与有效期，过期失效，不外泄密钥与桶凭证。
     *
     * @param key    存储键
     * @param expiry 有效期
     * @return 可直接下载的临时 URL
     */
    String presignedGetUrl(String key, Duration expiry);

    /** 删除对象（物理删除存储侧对象）。 */
    void remove(String key);
}
