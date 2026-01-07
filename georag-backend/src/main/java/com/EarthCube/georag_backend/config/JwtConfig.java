package com.EarthCube.georag_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * 对应 application.yml 中的 jwt 前缀配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * 密钥
     * 对应 jwt.secret
     */
    private String secret;

    /**
     * 过期时间 (毫秒)
     * 对应 jwt.expiration
     */
    private Long expiration;

    /**
     * 请求头名称 (默认 Authorization)
     * 对应 jwt.token-header
     */
    private String tokenHeader = "Authorization";

    /**
     * Token 前缀 (默认 "Bearer ")
     * 对应 jwt.token-head
     */
    private String tokenHead = "Bearer ";
}