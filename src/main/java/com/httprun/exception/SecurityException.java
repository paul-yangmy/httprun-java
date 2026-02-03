package com.httprun.exception;

/**
 * 安全异常
 * 用于命令注入检测、参数校验失败等安全相关的异常
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
