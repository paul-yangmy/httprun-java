package com.httprun.dto.request;

import com.httprun.entity.CommandConfig;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.ExecutionMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建命令请求
 */
@Data
@Schema(description = "创建命令请求对象")
public class CreateCommandRequest {

    /**
     * 命令名称
     */
    @Schema(description = "命令名称（唯一标识）", example = "deploy-app", required = true)
    @NotBlank(message = "命令名称不能为空")
    private String name;

    /**
     * 命令路径
     */
    @Schema(description = "命令路径（用于脚本文件路径）", example = "/scripts/deploy.sh")
    private String path;

    /**
     * 命令描述
     */
    @Schema(description = "命令描述信息", example = "部署应用到生产环境")
    private String description;

    /**
     * 命令配置（包含命令模板、参数定义、环境变量）
     */
    @Schema(description = "命令配置，包含命令模板、参数定义和环境变量")
    private CommandConfig commandConfig;

    /**
     * 执行模式
     */
    @Schema(description = "执行模式（LOCAL/SSH/AGENT）", example = "LOCAL")
    private ExecutionMode executionMode;

    /**
     * 命令分组
     */
    @Schema(description = "命令分组名称", example = "deployment")
    private String groupName;

    /**
     * 标签
     */
    @Schema(description = "命令标签，多个标签用逗号分隔", example = "docker,production")
    private String tags;

    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 远程执行配置（SSH 模式使用）
     */
    @Schema(description = "远程执行配置（SSH 模式使用），包含主机地址、端口、用户名、密码或密钥")
    private RemoteConfig remoteConfig;
}
