package com.httprun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HttpRun 应用启动类
 * 基于 HTTP API 的 Shell 命令网关系统
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class HttpRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(HttpRunApplication.class, args);
    }
}
