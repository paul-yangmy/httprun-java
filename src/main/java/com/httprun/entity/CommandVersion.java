package com.httprun.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 命令版本历史实体
 */
@Data
@Entity
@Table(name = "command_versions", indexes = {
        @Index(name = "idx_cv_name_version", columnList = "command_name, version DESC")
})
public class CommandVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的命令名称
     */
    @Column(name = "command_name", nullable = false, length = 100)
    private String commandName;

    /**
     * 版本号（单命令递增）
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * 命令配置快照（CreateCommandRequest JSON）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private String snapshot;

    /**
     * 变更说明
     */
    @Column(name = "change_note", length = 500)
    private String changeNote;

    /**
     * 变更时间
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
