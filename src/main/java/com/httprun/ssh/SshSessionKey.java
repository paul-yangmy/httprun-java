package com.httprun.ssh;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * SSH 连接池的 Key，用于标识不同的目标主机连接
 * <p>
 * 相同的 host + port + username 视为同一个连接池分组
 */
@Getter
@EqualsAndHashCode
@ToString
public class SshSessionKey {

    private final String host;
    private final int port;
    private final String username;

    public SshSessionKey(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * 生成用于日志和指标的标识字符串
     */
    public String toLabel() {
        return username + "@" + host + ":" + port;
    }
}
