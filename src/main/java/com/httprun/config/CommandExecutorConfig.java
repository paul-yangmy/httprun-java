package com.httprun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 命令执行器配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "command.executor")
public class CommandExecutorConfig {

    /**
     * 默认执行超时时间（秒）
     */
    private int defaultTimeout = 60;

    /**
     * 最大执行超时时间（秒）
     */
    private int maxTimeout = 3600;

    /**
     * 最大并发执行数
     */
    private int maxConcurrency = 10;

    /**
     * 执行队列大小
     */
    private int queueSize = 100;

    /**
     * 队列等待超时时间（秒）
     */
    private int queueTimeout = 30;

    /**
     * 是否启用命令审计
     */
    private boolean enableAudit = true;

    /**
     * 工作目录
     */
    private String workingDirectory = "/tmp";

    /**
     * 是否允许 Shell 执行
     */
    private boolean allowShell = false;
}
