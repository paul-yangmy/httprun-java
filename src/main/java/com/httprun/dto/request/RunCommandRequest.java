package com.httprun.dto.request;

import com.httprun.entity.EnvVar;
import com.httprun.entity.RemoteConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 运行命令请求
 */
@Data
@Schema(description = "运行命令请求对象")
public class RunCommandRequest {

    /**
     * 命令名称
     */
    @Schema(description = "命令名称", example = "deploy-app", required = true)
    private String name;

    /**
     * 命令参数列表
     */
    @Schema(description = "命令参数列表", example = "[{\"name\":\"env\",\"value\":\"prod\"},{\"name\":\"version\",\"value\":\"1.0.0\"}]")
    private List<ParamInput> params;

    /**
     * 环境变量
     */
    @Schema(description = "环境变量列表", example = "[{\"name\":\"NODE_ENV\",\"value\":\"production\"}]")
    private List<EnvVar> env;

    /**
     * 是否异步执行
     */
    @Schema(description = "是否异步执行（异步时立即返回，不等待命令完成）", example = "false", defaultValue = "false")
    private Boolean async = false;

    /**
     * 超时时间（秒），覆盖命令默认超时
     */
    @Schema(description = "超时时间（秒），覆盖命令默认超时配置", example = "300")
    private Integer timeout;

    /**
     * 远程执行配置（SSH/Agent 模式）
     */
    @Schema(description = "远程执行配置（用于 SSH 或 Agent 模式）")
    private RemoteConfig remoteConfig;

    @Data
    @Schema(description = "命令参数输入")
    public static class ParamInput {
        @Schema(description = "参数名称", example = "env", required = true)
        private String name;

        @Schema(description = "参数值", example = "prod", required = true)
        private Object value;
    }
}
