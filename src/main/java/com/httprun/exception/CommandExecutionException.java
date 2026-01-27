package com.httprun.exception;

import com.httprun.enums.ErrorCode;
import lombok.Getter;

/**
 * 命令执行异常
 */
@Getter
public class CommandExecutionException extends BusinessException {

    public CommandExecutionException() {
        super(ErrorCode.COMMAND_EXECUTION_FAILED);
    }

    public CommandExecutionException(String detail) {
        super(ErrorCode.COMMAND_EXECUTION_FAILED, detail);
    }
}
