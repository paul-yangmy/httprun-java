package com.httprun.executor;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;

import java.util.concurrent.CompletableFuture;

/**
 * 命令执行器接口
 */
public interface CommandExecutor {

    /**
     * 同步执行命令
     */
    CommandExecutionResult execute(String command, RunCommandRequest request, int timeoutSeconds);

    /**
     * 异步执行命令
     */
    CompletableFuture<CommandExecutionResult> executeAsync(String command, RunCommandRequest request,
            int timeoutSeconds);

    /**
     * 检查执行器是否可用
     */
    boolean isAvailable();
}
