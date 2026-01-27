package com.httprun.dto.request;

import com.httprun.entity.CommandConfig;
import com.httprun.enums.ExecutionMode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建命令请求
 */
@Data
public class CreateCommandRequest {

    /**
     * 命令名称
     */
    @NotBlank(message = "命令名称不能为空")
    private String name;

    /**
     * 命令路径
     */
    private String path;

    /**
     * 命令描述
     */
    private String description;

    /**
     * 命令配置（包含命令模板、参数定义、环境变量）
     */
    private CommandConfig commandConfig;

    /**
     * 执行模式
     */
    private ExecutionMode executionMode;

    /**
     * 命令分组
     */
    private String groupName;

    /**
     * 标签
     */
    private String tags;

    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;
}
