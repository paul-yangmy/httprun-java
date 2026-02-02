package com.httprun.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
     * 过期时间（Unix 时间戳，null 表示永久有效）
     */
    @Column(nullable = true)
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
     * 允许执行的开始时间（每日，格式：HH:mm）
     */
    @Column(length = 5)
    private String allowedStartTime;

    /**
     * 允许执行的结束时间（每日，格式：HH:mm）
     */
    @Column(length = 5)
    private String allowedEndTime;

    /**
     * 允许执行的星期几（JSON 数组，如 "1,2,3,4,5" 表示周一到周五）
     * 1=周一, 2=周二, ..., 7=周日
     */
    @Column(length = 50)
    private String allowedWeekdays;

    /**
     * 备注信息
     */
    @Column(length = 500)
    private String remark;

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

    /**
     * 检查当前时间是否在允许的时间范围内
     */
    @Transient
    public boolean isWithinAllowedTimeRange() {
        LocalDateTime now = LocalDateTime.now();

        // 检查星期几限制
        if (allowedWeekdays != null && !allowedWeekdays.isEmpty()) {
            int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=周一, 7=周日
            String[] days = allowedWeekdays.split(",");
            boolean dayAllowed = false;
            for (String day : days) {
                if (String.valueOf(currentDayOfWeek).equals(day.trim())) {
                    dayAllowed = true;
                    break;
                }
            }
            if (!dayAllowed) {
                return false;
            }
        }

        // 检查时间范围限制
        if (allowedStartTime != null && !allowedStartTime.isEmpty()
                && allowedEndTime != null && !allowedEndTime.isEmpty()) {
            LocalTime currentTime = now.toLocalTime();
            LocalTime startTime = LocalTime.parse(allowedStartTime);
            LocalTime endTime = LocalTime.parse(allowedEndTime);

            // 处理跨夜情况（如 22:00 - 06:00）
            if (startTime.isAfter(endTime)) {
                // 跨夜：当前时间应该在开始时间之后 或 在结束时间之前
                if (currentTime.isBefore(startTime) && currentTime.isAfter(endTime)) {
                    return false;
                }
            } else {
                // 正常情况：当前时间应该在开始和结束时间之间
                if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
                    return false;
                }
            }
        }

        return true;
    }
}
