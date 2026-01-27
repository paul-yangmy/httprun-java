package com.httprun.dto.response;

import com.httprun.entity.CommandConfig;
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

    private String groupName;

    private String tags;

    private Integer timeoutSeconds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
