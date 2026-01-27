package com.httprun.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 */
@Component
public class SecurityUtils {

    /**
     * 获取当前认证用户
     */
    public static JwtUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal) {
            return (JwtUserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null && user.admin();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null ? user.name() : null;
    }

    /**
     * 获取当前用户的 Subject（授权范围）
     */
    public static String getCurrentSubject() {
        JwtUserPrincipal user = getCurrentUser();
        return user != null ? user.subject() : null;
    }

    /**
     * 检查当前用户是否有权限执行指定命令
     */
    public static boolean hasCommandPermission(String commandName) {
        JwtUserPrincipal user = getCurrentUser();
        if (user == null) {
            return false;
        }

        // 管理员有所有权限
        if (user.admin()) {
            return true;
        }

        String subject = user.subject();
        if (subject == null) {
            return false;
        }

        // * 表示所有命令
        if ("*".equals(subject)) {
            return true;
        }

        // 检查命令是否在授权列表中
        String[] allowedCommands = subject.split(",");
        for (String allowed : allowedCommands) {
            if (allowed.trim().equals(commandName)) {
                return true;
            }
        }

        return false;
    }
}
