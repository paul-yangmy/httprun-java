package com.httprun.cli;

import com.httprun.dto.request.CreateTokenRequest;
import com.httprun.entity.Token;
import com.httprun.exception.BusinessException;
import com.httprun.enums.ErrorCode;
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
 * 
 * 安全限制：
 * - 如果数据库中已存在有效的管理员 Token，将拒绝创建新的管理员 Token
 * - 这确保了管理员账号的唯一性，防止重复初始化导致的安全漏洞
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
            int expiresInHours = Integer.parseInt(getArgValue(args, "--expires", "0")); // 默认永不过期

            // 构建创建请求 - 管理员 Token 不设置时间范围限制
            CreateTokenRequest request = new CreateTokenRequest();
            request.setName(name);
            request.setCommands(null); // 管理员拥有所有命令权限
            request.setIsAdmin(true);
            request.setExpiresIn(expiresInHours); // 0 表示永不过期（默认 1 年）
            request.setRemark("系统初始化生成的管理员 Token");
            // 不设置时间范围限制，确保管理员 Token 永久有效
            request.setAllowedStartTime(null);
            request.setAllowedEndTime(null);
            request.setAllowedWeekdays(null);

            // 生成管理员 Token（TokenService 会检查管理员唯一性）
            Token token = tokenService.createToken(request);

            log.info("");
            log.info("========================================");
            log.info("管理员 Token 生成成功!");
            log.info("========================================");
            log.info("Token ID:     {}", token.getId());
            log.info("Token Name:   {}", token.getName());
            log.info("Subject:      {}", token.getSubject());
            log.info("Is Admin:     {}", token.getIsAdmin());
            log.info("Expires At:   {}", java.time.Instant.ofEpochSecond(token.getExpiresAt()));
            log.info("Time Limit:   无限制（永久有效）");
            log.info("");
            log.info("JWT Token:");
            log.info("----------------------------------------");
            System.out.println(token.getJwtToken());
            log.info("----------------------------------------");
            log.info("");
            log.info("请保存此 Token，后续将无法再次查看完整内容!");
            log.info("========================================");

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.ADMIN_ALREADY_EXISTS) {
                log.warn("========================================");
                log.warn("管理员 Token 生成已跳过!");
                log.warn("========================================");
                log.warn("原因: 数据库中已存在有效的管理员账号");
                log.warn("如需重新生成管理员 Token，请先通过 API 撤销现有的管理员 Token");
                log.warn("========================================");
                // 不抛出异常，让应用继续正常启动
                return;
            }
            log.error("生成管理员 Token 失败: {}", e.getMessage(), e);
            throw e;
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
