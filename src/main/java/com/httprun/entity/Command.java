package com.httprun.entity;

import com.httprun.enums.CommandStatus;
import com.httprun.enums.ExecutionMode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 命令实体
 */
@Data
@Entity
@Table(name = "commands", indexes = {
        @Index(name = "idx_command_name", columnList = "name", unique = true),
        @Index(name = "idx_command_status", columnList = "status")
})
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 命令名称（唯一）
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 命令路径（可选）
     */
    @Column(unique = true, length = 200)
    private String path;

    /**
     * 命令描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 命令状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommandStatus status = CommandStatus.ACTIVE;

    /**
     * 命令配置 JSON，包含：
     * - command: 命令模板
     * - params: 参数定义列表
     * - env: 环境变量列表
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private CommandConfig commandConfig;

    /**
     * 执行模式：LOCAL, SSH, AGENT
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExecutionMode executionMode = ExecutionMode.LOCAL;

    /**
     * 远程执行配置（SSH/Agent 模式使用）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private RemoteConfig remoteConfig;

    /**
     * 命令分组
     */
    @Column(length = 50)
    private String groupName;

    /**
     * 标签（逗号分隔）
     */
    @Column(length = 200)
    private String tags;

    /**
     * 执行超时时间（秒）
     */
    @Column
    private Integer timeoutSeconds = 30;

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
