package com.httprun.dto.request;

import com.httprun.entity.EnvVar;
import com.httprun.entity.RemoteConfig;
import lombok.Data;

import java.util.List;

/**
 * 运行命令请求
 */
@Data
public class RunCommandRequest {

    /**
     * 命令名称
     */
    private String name;

    /**
     * 命令参数列表
     */
    private List<ParamInput> params;

    /**
     * 环境变量
     */
    private List<EnvVar> env;

    /**
     * 是否异步执行
     */
    private Boolean async = false;

    /**
     * 超时时间（秒），覆盖命令默认超时
     */
    private Integer timeout;

    /**
     * 远程执行配置（SSH/Agent 模式）
     */
    private RemoteConfig remoteConfig;

    @Data
    public static class ParamInput {
        private String name;
        private Object value;
    }
}
