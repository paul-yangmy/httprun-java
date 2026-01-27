package com.httprun.aspect;

import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.AccessLogService;
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

/**
 * 访问日志切面
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

        String tokenId = null;
        String path = request != null ? request.getRequestURI() : "unknown";
        String ip = request != null ? getClientIp(request) : "unknown";
        String method = request != null ? request.getMethod() : "unknown";

        // 获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal user) {
            tokenId = user.name();
        }

        String requestBody = null;
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                requestBody = objectMapper.writeValueAsString(args[0]);
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

            // 异步记录日志
            try {
                accessLogService.logAccess(tokenId, path, ip, method,
                        requestBody, responseBody, statusCode, duration);
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
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
