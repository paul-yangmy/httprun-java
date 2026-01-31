package com.httprun.aspect;

import com.httprun.dto.AuditContext;
import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.AccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
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

    @Pointcut("within(com.httprun.controller..*)")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();

        // 生成请求 ID 用于链路追踪
        String requestId = generateRequestId(request);

        String tokenId = null;
        String path = request != null ? request.getRequestURI() : "unknown";
        String ip = request != null ? getClientIp(request) : "unknown";
        String method = request != null ? request.getMethod() : "unknown";

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
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉 HttpServletRequest/Response 等不可序列化对象
                for (Object arg : args) {
                    if (arg != null && isSerializable(arg)) {
                        requestBody = objectMapper.writeValueAsString(arg);
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
     */
    private String extractCommandName(ProceedingJoinPoint joinPoint, String path) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 尝试从注解中获取路径作为命令名
            if (method.isAnnotationPresent(GetMapping.class)) {
                String[] values = method.getAnnotation(GetMapping.class).value();
                if (values.length > 0)
                    return "GET:" + values[0];
            }
            if (method.isAnnotationPresent(PostMapping.class)) {
                String[] values = method.getAnnotation(PostMapping.class).value();
                if (values.length > 0)
                    return "POST:" + values[0];
            }
            if (method.isAnnotationPresent(PutMapping.class)) {
                String[] values = method.getAnnotation(PutMapping.class).value();
                if (values.length > 0)
                    return "PUT:" + values[0];
            }
            if (method.isAnnotationPresent(DeleteMapping.class)) {
                String[] values = method.getAnnotation(DeleteMapping.class).value();
                if (values.length > 0)
                    return "DELETE:" + values[0];
            }

            // 回退：使用类名.方法名
            return joinPoint.getSignature().getDeclaringType().getSimpleName()
                    + "." + method.getName();
        } catch (Exception e) {
            log.debug("Failed to extract command name", e);
        }

        // 最后回退：从路径中提取
        if (path != null && path.startsWith("/api/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return parts[2]; // 返回 /api/ 后面的部分
            }
        }
        return path;
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
