package com.httprun.entity;

import lombok.Data;

/**
 * 环境变量定义
 */
@Data
public class EnvVar {
    /**
     * 环境变量名
     */
    private String name;

    /**
     * 环境变量值
     */
    private String value;
}
