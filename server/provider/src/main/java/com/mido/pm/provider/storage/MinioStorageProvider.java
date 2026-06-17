package com.mido.pm.provider.storage;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 实现 {@link StorageProvider}。桶在首次写入时惰性确保（避免启动期强依赖 MinIO）。
 * 预签名 URL 由 SDK 本地签名生成，限时有效，不外泄密钥。
 */
@Component
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady = false;

    public MinioStorageProvider(MinioClient client,
                                @Value("${mido.minio.bucket:mido-pm}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream data, long size, String contentType) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType == null || contentType.isBlank()
                            ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "对象上传失败: " + e.getMessage());
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration expiry) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "预签名URL生成失败: " + e.getMessage());
        }
    }

    @Override
    public String presignedPutUrl(String key, Duration expiry) {
        ensureBucket(); // 客户端 PUT 前确保桶存在
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "预签名上传URL生成失败: " + e.getMessage());
        }
    }

    @Override
    public void remove(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "对象删除失败: " + e.getMessage());
        }
    }

    /** 首次写入时确保桶存在（双检），不在启动期发起网络调用。 */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                bucketReady = true;
            } catch (Exception e) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "存储桶初始化失败: " + e.getMessage());
            }
        }
    }
}
