package com.xuecheng.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;
    @Bean
    public MinioClient minioClient(){
        MinioClient minioClient =
                MinioClient.builder().
                        endpoint("http://192.168.101.65:9000")
                        .credentials("minioadmin","minioadmin")
                        .build();
        return minioClient;
    }
}
