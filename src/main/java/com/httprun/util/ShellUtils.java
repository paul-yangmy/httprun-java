package com.httprun.util;

import java.util.regex.Pattern;

/**
 * Shell 工具类
 */
public class ShellUtils {

    // 危险字符模式
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "[;&|`$(){}\\[\\]<>\\\\'\"]");

    // 命令注入模式
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(&&|\\|\\||;|`|\\$\\(|\\$\\{|>|<|\\||&)");

    /**
     * 检查命令是否包含危险字符
     */
    public static boolean containsDangerousChars(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return DANGEROUS_PATTERN.matcher(input).find();
    }

    /**
     * 检查是否存在命令注入风险
     */
    public static boolean hasInjectionRisk(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * 清理危险字符
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return DANGEROUS_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 转义 Shell 参数
     */
    public static String escapeShellArg(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "''";
        }

        // 如果不包含特殊字符，直接返回
        if (!containsDangerousChars(arg)) {
            return arg;
        }

        // 使用单引号包围，并转义内部的单引号
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    /**
     * 转义用于 shell 命令的字符串
     */
    public static String escapeForShell(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\'':
                    sb.append("'\\''");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '$':
                    sb.append("\\$");
                    break;
                case '`':
                    sb.append("\\`");
                    break;
                case '!':
                    sb.append("\\!");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 验证参数是否安全
     */
    public static boolean isValidParam(String param) {
        if (param == null || param.isEmpty()) {
            return true; // 空参数视为安全
        }

        // 检查是否包含命令注入模式
        if (hasInjectionRisk(param)) {
            return false;
        }

        // 检查长度
        if (param.length() > 1000) {
            return false;
        }

        return true;
    }

    /**
     * 验证命令名是否合法
     */
    public static boolean isValidCommandName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // 只允许字母、数字、下划线、横线
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
}
