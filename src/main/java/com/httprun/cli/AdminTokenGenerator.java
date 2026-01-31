package com.httprun.cli;

import com.httprun.entity.Token;
import com.httprun.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 管理员 Token 生成器
 * 
 * 启动时通过设置 httprun.init-admin-token=true 来生成管理员 Token
 * 
 * 使用方式:
 * java -jar httprun.jar --httprun.init-admin-token=true
 * 或在配置文件中设置:
 * httprun:
 * init-admin-token: true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "httprun.init-admin-token", havingValue = "true")
public class AdminTokenGenerator implements CommandLineRunner {

    private final TokenService tokenService;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("开始生成管理员 Token...");
        log.info("========================================");

        try {
            // 解析命令行参数
            String name = getArgValue(args, "--name", "admin");
            String subject = getArgValue(args, "--subject", "admin");
            int expiresInHours = Integer.parseInt(getArgValue(args, "--expires", "8760")); // 默认 1 年

            // 生成管理员 Token
            Token token = tokenService.createToken(name, subject, true, expiresInHours);

            log.info("");
            log.info("========================================");
            log.info("管理员 Token 生成成功!");
            log.info("========================================");
            log.info("Token ID:     {}", token.getId());
            log.info("Token Name:   {}", token.getName());
            log.info("Subject:      {}", token.getSubject());
            log.info("Is Admin:     {}", token.getIsAdmin());
            log.info("Expires At:   {}", java.time.Instant.ofEpochSecond(token.getExpiresAt()));
            log.info("");
            log.info("JWT Token:");
            log.info("----------------------------------------");
            System.out.println(token.getJwtToken());
            log.info("----------------------------------------");
            log.info("");
            log.info("请保存此 Token，后续将无法再次查看完整内容!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("生成管理员 Token 失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getArgValue(String[] args, String key, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(key + "=")) {
                return arg.substring(key.length() + 1);
            }
        }
        return defaultValue;
    }
}
