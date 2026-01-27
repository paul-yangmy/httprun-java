package com.httprun.entity;

import lombok.Data;

/**
 * 远程执行配置（SSH/Agent 模式使用）
 */
@Data
public class RemoteConfig {
    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 用户名
     */
    private String username;

    /**
     * SSH 私钥
     */
    private String privateKey;

    /**
     * 密码
     */
    private String password;

    /**
     * SSH 密钥 ID
     */
    private Long sshKeyId;

    /**
     * Agent 标识
     */
    private String agentId;
}
