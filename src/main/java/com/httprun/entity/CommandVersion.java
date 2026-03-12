package com.httprun.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

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
    @Column(nullable = false, columnDefinition = "jsonb")
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
