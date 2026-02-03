package com.httprun.dto.response;

import com.httprun.entity.CommandConfig;
import com.httprun.entity.RemoteConfig;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命令响应
 */
@Data
public class CommandResponse {

    private Long id;

    private String name;

    private String path;

    private String description;

    private String status;

    private CommandConfig commandConfig;

    private String executionMode;

    private RemoteConfig remoteConfig;

    private String groupName;

    private String tags;

    private Integer timeoutSeconds;

    /**
     * 危险等级：0=安全, 1=警告, 2=高危
     */
    private Integer dangerLevel;

    /**
     * 危险警告信息（当 dangerLevel > 0 时有值）
     */
    private String dangerWarning;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
