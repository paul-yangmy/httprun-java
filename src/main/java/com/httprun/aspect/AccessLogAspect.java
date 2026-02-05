package com.httprun.aspect;

import com.httprun.dto.AuditContext;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.repository.CommandRepository;
import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.AccessLogService;
import com.httprun.util.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

/**
 * 访问日志切面 - 增强版
 * 收集完整的审计信息：IP、User-Agent、Referer、来源、请求链路等
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AccessLogAspect {

    private final AccessLogService accessLogService;
    private final ObjectMapper objectMapper;
    private final CommandRepository commandRepository;

    @Pointcut("within(com.httprun.controller..*)")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();

        String path = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";

        // 判断是否需要记录日志（只记录有意义的操作）
        if (!shouldLog(method, path)) {
            return joinPoint.proceed();
        }

        // 生成请求 ID 用于链路追踪
        String requestId = generateRequestId(request);

        String tokenId = null;
        String ip = request != null ? getClientIp(request) : "unknown";

        // 审计增强：获取额外的请求信息
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String referer = request != null ? request.getHeader("Referer") : null;
        String forwardedFor = request != null ? request.getHeader("X-Forwarded-For") : null;

        // 推断请求来源
        String source = AuditContext.inferSource(userAgent);

        // 获取命令名称（从路径或方法注解中提取）
        String commandName = extractCommandName(joinPoint, path);

        // 获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal user) {
            tokenId = user.name();
        }

        String requestBody = null;
        Command command = null;
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉 HttpServletRequest/Response 等不可序列化对象
                for (Object arg : args) {
                    if (arg != null && isSerializable(arg)) {
                        // 如果是 RunCommandRequest，尝试加载命令定义进行脱敏
                        if (arg instanceof RunCommandRequest runCommandRequest) {
                            command = loadCommand(runCommandRequest.getName());
                            if (command != null) {
                                requestBody = SensitiveDataMasker.maskRequest(command, runCommandRequest);
                            } else {
                                requestBody = objectMapper.writeValueAsString(arg);
                            }
                        } else {
                            requestBody = objectMapper.writeValueAsString(arg);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to serialize request body", e);
        }

        Object result = null;
        String responseBody = null;
        int statusCode = 200;

        try {
            result = joinPoint.proceed();

            if (result != null) {
                try {
                    responseBody = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    log.debug("Failed to serialize response", e);
                }
            }

            return result;

        } catch (Exception e) {
            statusCode = 500;
            responseBody = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 构建审计上下文并异步记录日志
            try {
                AuditContext context = AuditContext.builder()
                        .tokenId(tokenId)
                        .path(path)
                        .ip(ip)
                        .method(method)
                        .userAgent(userAgent)
                        .referer(referer)
                        .source(source)
                        .forwardedFor(forwardedFor)
                        .requestId(requestId)
                        .commandName(commandName)
                        .request(requestBody)
                        .response(responseBody)
                        .statusCode(statusCode)
                        .duration(duration)
                        .build();

                accessLogService.logAccess(context);
            } catch (Exception e) {
                log.warn("Failed to log access", e);
            }
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个 IP 的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 生成请求 ID
     * 优先使用请求头中的 X-Request-ID，否则生成新的 UUID
     */
    private String generateRequestId(HttpServletRequest request) {
        if (request != null) {
            String existingId = request.getHeader("X-Request-ID");
            if (existingId != null && !existingId.isEmpty()) {
                return existingId;
            }
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 从切入点或路径中提取命令名称
     * 只有在真正执行命令时才返回命令名，其他 API 调用返回 null
     */
    private String extractCommandName(ProceedingJoinPoint joinPoint, String path) {
        // 只有 /api/run 才是命令执行，从请求参数中提取命令名
        if (path != null && path.equals("/api/run")) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null) {
                    for (Object arg : args) {
                        if (arg instanceof RunCommandRequest runCommandRequest) {
                            return runCommandRequest.getName();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract command name from request", e);
            }
        }

        // WebSocket 流式执行也是命令执行，但日志由 WebSocket 单独处理
        // 其他 API 调用不设置 commandName
        return null;
    }

    /**
     * 判断是否需要记录访问日志
     * 只记录有意义的操作，过滤掉查询类请求
     */
    private boolean shouldLog(String method, String path) {
        // 始终记录命令执行
        if ("/api/run".equals(path)) {
            return true;
        }

        // 记录所有修改类操作（POST、PUT、DELETE、PATCH）
        if ("POST".equalsIgnoreCase(method) ||
                "PUT".equalsIgnoreCase(method) ||
                "DELETE".equalsIgnoreCase(method) ||
                "PATCH".equalsIgnoreCase(method)) {
            return true;
        }

        // 不记录 GET 请求（查询、获取列表等）
        // 这些请求通常是页面刷新、获取数据，不需要记录
        return false;
    }

    /**
     * 加载命令定义（用于脱敏）
     */
    private Command loadCommand(String commandName) {
        try {
            return commandRepository.findByName(commandName).orElse(null);
        } catch (Exception e) {
            log.debug("Failed to load command for masking: {}", commandName, e);
            return null;
        }
    }

    /**
     * 检查对象是否可序列化（排除 Servlet 相关类）
     */
    private boolean isSerializable(Object obj) {
        if (obj == null)
            return false;
        String className = obj.getClass().getName();
        return !className.startsWith("jakarta.servlet")
                && !className.startsWith("org.springframework.web");
    }
}
