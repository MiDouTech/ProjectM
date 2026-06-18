package com.mido.pm.provider.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端装配。仅构建客户端（不发起网络），桶的存在性在首次上传时惰性确保，
 * 避免 MinIO 未就绪时拖垮应用启动。连接参数取自 mido.minio.*（见 application.yml）。
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${mido.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${mido.minio.access-key:mido}") String accessKey,
            @Value("${mido.minio.secret-key:mido12345}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
