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
        @Index(name = "idx_accesslog_created", columnList = "createdAt")
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
