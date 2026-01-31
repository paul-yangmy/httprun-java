package com.httprun.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志实体
 */
@Data
@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_accesslog_token", columnList = "tokenId"),
        @Index(name = "idx_accesslog_path", columnList = "path"),
        @Index(name = "idx_accesslog_created", columnList = "createdAt"),
        @Index(name = "idx_accesslog_ip", columnList = "ip")
})
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token ID
     */
    @Column(length = 500)
    private String tokenId;

    /**
     * 请求路径
     */
    @Column(nullable = false, length = 200)
    private String path;

    /**
     * 客户端 IP
     */
    @Column(length = 50)
    private String ip;

    /**
     * 请求方法
     */
    @Column(length = 20)
    private String method;

    // ========== 审计增强字段 ==========

    /**
     * User-Agent 浏览器/客户端标识
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * Referer 请求来源页面
     */
    @Column(length = 500)
    private String referer;

    /**
     * 请求来源（WEB/API/CLI）
     */
    @Column(length = 20)
    private String source;

    /**
     * X-Forwarded-For 原始IP链
     */
    @Column(length = 200)
    private String forwardedFor;

    /**
     * 请求ID（用于链路追踪）
     */
    @Column(length = 50)
    private String requestId;

    /**
     * 命令名称（便于统计）
     */
    @Column(length = 100)
    private String commandName;

    // ========== 原有字段 ==========

    /**
     * 请求内容
     */
    @Column(columnDefinition = "TEXT")
    private String request;

    /**
     * 响应内容
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String response;

    /**
     * 响应状态码
     */
    @Column
    private Integer statusCode;

    /**
     * 执行耗时（毫秒）
     */
    @Column
    private Long duration;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
