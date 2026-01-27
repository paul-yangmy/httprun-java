package com.httprun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 密钥
     */
    private String secret = "httprun-secret-key-at-least-32-characters-long";

    /**
     * Token 有效期（毫秒），默认 24 小时
     */
    private long expiration = 86400000;

    /**
     * Token 刷新时间（毫秒），默认 12 小时
     */
    private long refreshTime = 43200000;

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token 请求头
     */
    private String headerName = "x-token";
}
