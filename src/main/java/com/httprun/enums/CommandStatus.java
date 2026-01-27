package com.httprun.enums;

/**
 * 命令状态枚举
 */
public enum CommandStatus {
    /**
     * 活跃状态，可以执行
     */
    ACTIVE,

    /**
     * 禁用状态，不可执行
     */
    DISABLED,

    /**
     * 已删除状态
     */
    DELETED
}
