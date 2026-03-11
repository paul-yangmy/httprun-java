package com.httprun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * HttpRun 应用启动类
 * 基于 HTTP API 的 Shell 命令网关系统
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class HttpRunApplication {
    public static void main(String[] args) {
        // 强制 stdout/stderr 使用 UTF-8，避免 Windows 下中文日志乱码
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        SpringApplication.run(HttpRunApplication.class, args);
    }
}
