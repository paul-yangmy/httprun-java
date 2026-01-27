package com.httprun.entity;

import lombok.Data;

import java.util.List;

/**
 * 命令配置（存储为 JSON）
 */
@Data
public class CommandConfig {
    /**
     * 命令模板
     */
    private String command;

    /**
     * 参数定义
     */
    private List<ParamDefine> params;

    /**
     * 环境变量
     */
    private List<EnvVar> env;
}
