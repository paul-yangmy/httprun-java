package com.httprun.enums;

/**
 * 命令执行模式枚举
 */
public enum ExecutionMode {
    /**
     * 本地执行
     */
    LOCAL,

    /**
     * SSH 远程执行
     */
    SSH,

    /**
     * Agent 执行
     */
    AGENT
}
