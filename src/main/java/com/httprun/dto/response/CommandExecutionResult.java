package com.httprun.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandExecutionResult {

    /**
     * 标准输出
     */
    private String stdout;

    /**
     * 标准错误
     */
    private String stderr;

    /**
     * 退出码
     */
    private int exitCode;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 异步任务 ID
     */
    private String taskId;

    /**
     * 创建错误结果
     */
    public static CommandExecutionResult error(String errorMessage) {
        return CommandExecutionResult.builder()
                .error(errorMessage)
                .exitCode(-1)
                .build();
    }
}
