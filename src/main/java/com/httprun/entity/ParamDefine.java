package com.httprun.entity;

import lombok.Data;

/**
 * 参数定义
 */
@Data
public class ParamDefine {
    /**
     * 参数名
     */
    private String name;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 参数类型: string, integer, boolean
     */
    private String type;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 是否必需
     */
    private boolean required;

    /**
     * 是否敏感参数（如密码、密钥等）
     * 敏感参数在日志中会被脱敏显示为 ***
     */
    private boolean sensitive;
}
