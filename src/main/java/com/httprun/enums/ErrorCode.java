package com.httprun.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误 1xxx
    SUCCESS(0, "成功"),
    UNKNOWN_ERROR(1000, "未知错误"),
    INVALID_PARAMETER(1001, "参数无效"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    OPERATION_FAILED(1003, "操作失败"),

    // 认证错误 2xxx
    UNAUTHORIZED(2000, "未授权"),
    TOKEN_INVALID(2001, "Token 无效"),
    TOKEN_EXPIRED(2002, "Token 已过期"),
    TOKEN_REVOKED(2003, "Token 已被撤销"),
    ACCESS_DENIED(2004, "访问被拒绝"),

    // 命令错误 3xxx
    COMMAND_NOT_FOUND(3000, "命令不存在"),
    COMMAND_DISABLED(3001, "命令已禁用"),
    COMMAND_EXECUTION_FAILED(3002, "命令执行失败"),
    COMMAND_TIMEOUT(3003, "命令执行超时"),
    COMMAND_PARAM_INVALID(3004, "命令参数无效"),
    COMMAND_ALREADY_EXISTS(3005, "命令已存在"),

    // Token 错误 4xxx
    TOKEN_NOT_FOUND(4000, "Token 不存在"),
    TOKEN_CREATION_FAILED(4001, "Token 创建失败"),
    ADMIN_ALREADY_EXISTS(4002, "管理员账号已存在，禁止重复创建"),

    // 执行器错误 5xxx
    EXECUTOR_UNAVAILABLE(5000, "执行器不可用"),
    EXECUTOR_QUEUE_FULL(5001, "执行队列已满"),
    SSH_CONNECTION_FAILED(5002, "SSH 连接失败"),

    // 安全错误 6xxx
    DANGEROUS_COMMAND(6000, "危险命令被拦截"),
    IP_NOT_ALLOWED(6001, "IP 地址不在白名单中"),
    RATE_LIMIT_EXCEEDED(6002, "请求频率超限");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
