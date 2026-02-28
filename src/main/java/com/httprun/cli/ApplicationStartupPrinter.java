package com.httprun.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 应用启动信息打印器
 * 在所有初始化完成后输出服务访问地址等关键信息
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
public class ApplicationStartupPrinter implements ApplicationRunner {

    private final Environment env;

    @Value("${server.port:8081}")
    private String port;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${spring.application.name:httprun}")
    private String appName;

    public ApplicationStartupPrinter(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String localIp = "localhost";
        String networkIp = "localhost";
        try {
            InetAddress address = InetAddress.getLocalHost();
            localIp = address.getHostAddress();
            networkIp = localIp;
        } catch (UnknownHostException ignored) {
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default";

        String ctx = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;

        log.info("\n----------------------------------------------------------\n\t" +
                "应用 '{}' 启动成功! \n\t" +
                "运行环境: \t{}\n\t" +
                "本地地址: \thttp://localhost:{}{}\n\t" +
                "外部地址: \thttp://{}:{}{}\n\t" +
                "Web 界面: \thttp://localhost:{}{}/admin\n\t" +
                "API 文档: \thttp://localhost:{}{}/swagger/index.html\n\t" +
                "健康检查: \thttp://localhost:{}{}/api/health\n\t" +
                "----------------------------------------------------------",
                appName,
                profile,
                port, ctx,
                networkIp, port, ctx,
                port, ctx,
                port, ctx,
                port, ctx);
    }
}
