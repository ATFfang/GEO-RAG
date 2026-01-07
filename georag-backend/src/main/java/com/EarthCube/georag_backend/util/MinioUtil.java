package com.EarthCube.georag_backend.util;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class MinioUtil {

    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucket-name:georag}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     * @param file 前端上传的文件
     * @return 文件的访问 URL
     */
    public String uploadAvatar(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        // 生成唯一文件名，防止覆盖
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String objectName = "avatar/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        try (InputStream inputStream = file.getInputStream()) {
            // 上传到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 拼接返回 URL
            // 注意：如果 MinIO 不在公网，这里可能需要配置 Nginx 代理地址
            return endpoint + "/" + bucketName + "/" + objectName;

        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传服务异常");
        }
    }
}
