package com.httprun.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token 实体
 */
@Data
@Entity
@Table(name = "tokens", indexes = {
        @Index(name = "idx_token_name", columnList = "name"),
        @Index(name = "idx_token_jwt", columnList = "jwtToken", unique = true)
})
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 授权的命令列表（逗号分隔）
     */
    @Column(nullable = false, length = 1000)
    private String subject;

    /**
     * 用户名/Token 名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 是否管理员
     */
    @Column(nullable = false)
    private Boolean isAdmin = false;

    /**
     * 签发时间（Unix 时间戳）
     */
    @Column(nullable = false)
    private Long issuedAt;

    /**
     * 过期时间（Unix 时间戳）
     */
    @Column(nullable = false)
    private Long expiresAt;

    /**
     * JWT Token 字符串
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String jwtToken;

    /**
     * Token 是否被撤销
     */
    @Column(nullable = false)
    private Boolean revoked = false;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
