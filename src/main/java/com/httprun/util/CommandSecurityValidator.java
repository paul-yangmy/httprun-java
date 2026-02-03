package com.httprun.util;

import com.httprun.entity.Command;
import com.httprun.entity.ParamDefine;
import com.httprun.exception.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 命令安全验证器
 * 
 * 提供全面的命令注入防护和高危命令检测功能：
 * 1. 参数值注入攻击检测（命令注入防护）
 * 2. 高危命令模式识别
 * 3. 危险字符过滤
 * 4. 参数白名单验证 - 禁止特殊字符
 * 5. 路径遍历防护
 */
@Slf4j
@Component
public class CommandSecurityValidator {

    // ===== 危险字符和模式定义 =====

    /**
     * 命令注入危险字符
     */
    private static final String[] DANGEROUS_CHARS = {
            ";", // 命令分隔符
            "|", // 管道符
            "&", // 后台执行/命令连接符
            "`", // 命令替换
            "$(", // 命令替换
            "${", // 变量展开
            "$((", // 算术展开
            "<", // 输入重定向
            ">", // 输出重定向
            ">>", // 追加重定向
            "<<", // Here-document
            "\n", // 换行（命令分隔）
            "\r", // 回车
            "\\", // 转义字符（需要谨慎处理）
    };

    /**
     * 命令注入攻击模式（正则表达式）
     */
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            // 命令替换
            Pattern.compile("\\$\\([^)]*\\)"), // $(command)
            Pattern.compile("`[^`]*`"), // `command`
            Pattern.compile("\\$\\{[^}]*}"), // ${variable}

            // 命令连接
            Pattern.compile("&&"), // cmd1 && cmd2
            Pattern.compile("\\|\\|"), // cmd1 || cmd2
            Pattern.compile("\\|(?!\\|)"), // cmd1 | cmd2 (pipe)
            Pattern.compile(";"), // cmd1; cmd2

            // 换行注入
            Pattern.compile("\\r?\\n"), // newline injection
            Pattern.compile("%0[aAdD]"), // URL encoded newline

            // 空字节注入
            Pattern.compile("\\x00"), // null byte
            Pattern.compile("%00"), // URL encoded null

            // 常见绕过技巧
            Pattern.compile("\\$IFS"), // IFS绕过空格
            Pattern.compile("\\{.*,.*\\}"), // bash brace expansion
            Pattern.compile("\\$\\{!.*\\}"), // indirect variable
            Pattern.compile("\\$\\(\\(.*\\)\\)"), // arithmetic expansion

            // Base64 编码执行
            Pattern.compile("base64\\s+-d"), // base64 decode
            Pattern.compile("\\|\\s*sh"), // pipe to shell
            Pattern.compile("\\|\\s*bash"), // pipe to bash

            // 路径遍历攻击模式（增强）
            Pattern.compile("\\.\\./"), // directory traversal ../
            Pattern.compile("\\.\\.\\\\"), // Windows路径遍历 ..\\
            Pattern.compile("%2e%2e[/%5c]", Pattern.CASE_INSENSITIVE), // URL编码路径遍历
            Pattern.compile("%252e%252e", Pattern.CASE_INSENSITIVE), // 双重URL编码
            Pattern.compile("\\.\\.%c0%af"), // 畸形UTF-8编码
            Pattern.compile("\\.\\.%c1%9c"), // 畸形UTF-8编码
            Pattern.compile("/etc/"), // sensitive path
            Pattern.compile("/root/"), // root home
            Pattern.compile("/home/[^/]+/\\."), // hidden files in home
            Pattern.compile("^/\\.\\."), // 以 /.. 开头
            Pattern.compile("(?:^|/)\\.\\.(?:/|$)"), // 任意位置的 ..

            // 特殊文件操作
            Pattern.compile(">/dev/"), // device files
            Pattern.compile("/proc/"), // proc filesystem
            Pattern.compile("/sys/"), // sys filesystem
            Pattern.compile("/boot/"), // boot partition
            Pattern.compile("/var/log/"), // system logs

            // Windows 敏感路径
            Pattern.compile("[a-zA-Z]:\\\\windows", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[a-zA-Z]:\\\\system32", Pattern.CASE_INSENSITIVE));

    // ===== 参数白名单验证模式 =====

    /**
     * 安全参数格式白名单（正则表达式）
     * 只有匹配这些模式的参数值才被认为是安全的
     */
    private static final Map<String, Pattern> PARAM_WHITELIST_PATTERNS = new HashMap<>();
    static {
        // 通用字母数字（允许空格、下划线、连字符）
        PARAM_WHITELIST_PATTERNS.put("alphanumeric", Pattern.compile("^[a-zA-Z0-9_\\-\\s]+$"));
        // 严格字母数字（不允许空格）
        PARAM_WHITELIST_PATTERNS.put("strict_alphanumeric", Pattern.compile("^[a-zA-Z0-9_\\-]+$"));
        // 文件名（允许点号但不允许路径分隔符）
        PARAM_WHITELIST_PATTERNS.put("filename", Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$"));
        // 安全路径（仅允许相对路径，无路径遍历）
        PARAM_WHITELIST_PATTERNS.put("safe_path",
                Pattern.compile("^(?!.*\\.\\.)(?!/)(?![a-zA-Z]:)[a-zA-Z0-9_/\\-\\.]+$"));
        // IPv4 地址
        PARAM_WHITELIST_PATTERNS.put("ipv4", Pattern
                .compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"));
        // 主机名/域名
        PARAM_WHITELIST_PATTERNS.put("hostname", Pattern.compile(
                "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$"));
        // 端口号
        PARAM_WHITELIST_PATTERNS.put("port", Pattern
                .compile("^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$"));
        // URL（简化版）
        PARAM_WHITELIST_PATTERNS.put("url",
                Pattern.compile("^https?://[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9](/[a-zA-Z0-9_\\-\\./]*)?$"));
        // 邮箱
        PARAM_WHITELIST_PATTERNS.put("email", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"));
        // 数字（含负数和小数）
        PARAM_WHITELIST_PATTERNS.put("number", Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$"));
        // 整数
        PARAM_WHITELIST_PATTERNS.put("integer", Pattern.compile("^-?[0-9]+$"));
        // 布尔值
        PARAM_WHITELIST_PATTERNS.put("boolean", Pattern.compile("^(true|false|yes|no|1|0)$", Pattern.CASE_INSENSITIVE));
        // 版本号
        PARAM_WHITELIST_PATTERNS.put("version", Pattern.compile("^[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9]+)?$"));
        // UUID
        PARAM_WHITELIST_PATTERNS.put("uuid",
                Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    /**
     * 禁止的特殊字符（用于白名单验证）
     */
    private static final Pattern FORBIDDEN_SPECIAL_CHARS = Pattern.compile(
            "[;|&`$(){}\\[\\]<>\"'\\\\\\n\\r\\u0000]");

    // ===== 高危命令定义 =====

    /**
     * 高危命令关键词（需要二次确认）
     */
    private static final Set<String> DANGEROUS_COMMANDS = new HashSet<>(Arrays.asList(
            // 删除类
            "rm", "rmdir", "unlink", "shred",
            "del", "erase", "rd", // Windows

            // 格式化/分区
            "mkfs", "fdisk", "parted", "dd",
            "format", // Windows

            // 系统控制
            "shutdown", "reboot", "poweroff", "halt", "init",

            // 权限/用户
            "chmod", "chown", "chgrp", "passwd", "useradd", "userdel", "usermod",
            "adduser", "deluser",

            // 网络危险操作
            "iptables", "ip6tables", "firewall-cmd", "ufw",
            "ifconfig", "ip", // 网络配置

            // 危险写入操作
            "truncate", "> /dev/", "mv /",

            // 进程控制
            "kill", "killall", "pkill",

            // 包管理（可能造成系统不稳定）
            "apt-get remove", "apt-get purge", "yum remove", "dnf remove",
            "pip uninstall", "npm uninstall",

            // 数据库危险操作
            "drop database", "drop table", "truncate table", "delete from",

            // 系统配置
            "systemctl stop", "systemctl disable", "service stop",
            "update-rc.d", "chkconfig"));

    /**
     * 高危命令模式（正则匹配）
     */
    private static final List<Pattern> DANGEROUS_COMMAND_PATTERNS = Arrays.asList(
            // rm 危险模式
            Pattern.compile("rm\\s+(-[rf]+\\s+)*(/|~|\\$HOME)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("rm\\s+(-[rf]+\\s+)*\\*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("rm\\s+-rf?\\s+--no-preserve-root", Pattern.CASE_INSENSITIVE),

            // dd 危险写入
            Pattern.compile("dd\\s+.*of=/dev/", Pattern.CASE_INSENSITIVE),

            // 危险的文件覆盖
            Pattern.compile(">\\s*/etc/", Pattern.CASE_INSENSITIVE),
            Pattern.compile(">\\s*/boot/", Pattern.CASE_INSENSITIVE),

            // 递归修改权限
            Pattern.compile("chmod\\s+-R\\s+777", Pattern.CASE_INSENSITIVE),
            Pattern.compile("chown\\s+-R\\s+.*\\s+/", Pattern.CASE_INSENSITIVE),

            // Fork bomb 检测
            Pattern.compile(":\\(\\)\\{\\s*:\\|:", Pattern.CASE_INSENSITIVE),

            // 历史清除
            Pattern.compile("history\\s+-c", Pattern.CASE_INSENSITIVE),
            Pattern.compile(">\\s*~/?\\.bash_history", Pattern.CASE_INSENSITIVE));

    // ===== 公共方法 =====

    /**
     * 验证参数值是否安全（防止命令注入）
     * 
     * @param paramName 参数名
     * @param value     参数值
     * @throws SecurityException 如果检测到危险内容
     */
    public void validateParamValue(String paramName, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        // 1. 检查危险字符
        for (String dangerous : DANGEROUS_CHARS) {
            if (value.contains(dangerous)) {
                log.warn("Security: Dangerous character '{}' found in parameter '{}', value: {}",
                        dangerous, paramName, maskValue(value));
                throw new SecurityException(
                        String.format("参数 '%s' 包含不允许的字符: %s", paramName, dangerous));
            }
        }

        // 2. 检查注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(value).find()) {
                log.warn("Security: Injection pattern detected in parameter '{}', pattern: {}, value: {}",
                        paramName, pattern.pattern(), maskValue(value));
                throw new SecurityException(
                        String.format("参数 '%s' 包含可疑的命令注入模式", paramName));
            }
        }

        // 3. 检查长度（防止缓冲区溢出攻击）
        if (value.length() > 10000) {
            log.warn("Security: Parameter '{}' value too long: {} chars", paramName, value.length());
            throw new SecurityException(
                    String.format("参数 '%s' 长度超过限制（最大 10000 字符）", paramName));
        }

        log.debug("Parameter '{}' passed security validation", paramName);
    }

    /**
     * 验证所有请求参数
     * 
     * @param command 命令配置
     * @param params  参数映射
     */
    public void validateAllParams(Command command, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }

        // 获取参数定义，用于类型检查
        Map<String, ParamDefine> paramDefines = new HashMap<>();
        if (command.getCommandConfig() != null && command.getCommandConfig().getParams() != null) {
            for (ParamDefine def : command.getCommandConfig().getParams()) {
                paramDefines.put(def.getName(), def);
            }
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();

            if (value != null) {
                String strValue = String.valueOf(value);

                // 基本安全校验
                validateParamValue(paramName, strValue);

                // 根据参数类型进行额外校验
                ParamDefine def = paramDefines.get(paramName);
                if (def != null) {
                    validateParamType(paramName, strValue, def);
                }
            }
        }
    }

    /**
     * 根据参数类型进行校验
     */
    private void validateParamType(String name, String value, ParamDefine def) {
        String type = def.getType();
        if (type == null) {
            return;
        }

        switch (type.toLowerCase()) {
            case "integer":
            case "int":
            case "number":
                if (!value.matches("^-?\\d+$")) {
                    throw new SecurityException(
                            String.format("参数 '%s' 必须是整数", name));
                }
                break;
            case "boolean":
            case "bool":
                if (!value.matches("^(true|false|1|0|yes|no)$")) {
                    throw new SecurityException(
                            String.format("参数 '%s' 必须是布尔值", name));
                }
                break;
            case "path":
            case "file":
                // 路径参数需要额外检查路径遍历
                if (value.contains("..") || value.startsWith("/etc") || value.startsWith("/root")) {
                    throw new SecurityException(
                            String.format("参数 '%s' 包含不允许的路径", name));
                }
                break;
            case "ip":
            case "ipaddress":
                if (!value.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
                        && !value.matches("^[a-fA-F0-9:]+$")) {
                    throw new SecurityException(
                            String.format("参数 '%s' 必须是有效的 IP 地址", name));
                }
                break;
            case "hostname":
                if (!value.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]*[a-zA-Z0-9]$")
                        || value.length() > 253) {
                    throw new SecurityException(
                            String.format("参数 '%s' 必须是有效的主机名", name));
                }
                break;
        }
    }

    /**
     * 检测命令是否为高危命令
     * 
     * @param commandTemplate 命令模板
     * @param renderedCommand 渲染后的实际命令
     * @return 危险等级：0=安全, 1=警告, 2=高危
     */
    public int detectDangerLevel(String commandTemplate, String renderedCommand) {
        if (commandTemplate == null && renderedCommand == null) {
            return 0;
        }

        String cmdToCheck = renderedCommand != null ? renderedCommand : commandTemplate;
        String lowerCmd = cmdToCheck.toLowerCase().trim();

        // 提取第一个命令词
        String firstWord = lowerCmd.split("\\s+")[0];
        // 去除可能的路径前缀
        if (firstWord.contains("/")) {
            firstWord = firstWord.substring(firstWord.lastIndexOf('/') + 1);
        }

        // 1. 检查高危命令模式
        for (Pattern pattern : DANGEROUS_COMMAND_PATTERNS) {
            if (pattern.matcher(cmdToCheck).find()) {
                log.warn("High-risk command pattern detected: {}", pattern.pattern());
                return 2; // 高危
            }
        }

        // 2. 检查高危命令关键词
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (dangerous.contains(" ")) {
                // 多词匹配（如 "drop database"）
                if (lowerCmd.contains(dangerous)) {
                    log.warn("Dangerous command keyword detected: {}", dangerous);
                    return 2; // 高危
                }
            } else {
                // 单词匹配
                if (firstWord.equals(dangerous)) {
                    log.info("Warning-level command detected: {}", dangerous);
                    return 1; // 警告
                }
            }
        }

        return 0; // 安全
    }

    /**
     * 获取高危命令的警告信息
     * 
     * @param commandTemplate 命令模板
     * @return 警告信息，如果不是高危命令则返回 null
     */
    public String getDangerWarning(String commandTemplate) {
        if (commandTemplate == null) {
            return null;
        }

        String lowerCmd = commandTemplate.toLowerCase().trim();
        String firstWord = lowerCmd.split("\\s+")[0];
        if (firstWord.contains("/")) {
            firstWord = firstWord.substring(firstWord.lastIndexOf('/') + 1);
        }

        // 根据命令类型返回不同的警告
        if (firstWord.equals("rm") || firstWord.equals("rmdir")) {
            return "此命令将删除文件或目录，操作不可逆！";
        } else if (firstWord.equals("dd")) {
            return "此命令可能覆写磁盘数据，请确认目标设备正确！";
        } else if (Arrays.asList("shutdown", "reboot", "poweroff", "halt").contains(firstWord)) {
            return "此命令将关闭或重启系统！";
        } else if (Arrays.asList("chmod", "chown").contains(firstWord)) {
            return "此命令将修改文件权限或所有者！";
        } else if (Arrays.asList("kill", "killall", "pkill").contains(firstWord)) {
            return "此命令将终止进程！";
        } else if (lowerCmd.contains("drop database") || lowerCmd.contains("drop table")) {
            return "此命令将删除数据库或数据表，操作不可逆！";
        } else if (lowerCmd.contains("truncate table") || lowerCmd.contains("delete from")) {
            return "此命令将清空数据表，操作不可逆！";
        } else if (Arrays.asList("iptables", "firewall-cmd", "ufw").contains(firstWord)) {
            return "此命令将修改防火墙规则，可能影响网络连接！";
        }

        return "此命令被标记为高危操作，请确认后执行！";
    }

    /**
     * 获取命令的高危命令列表（用于前端显示）
     * 
     * @return 高危命令集合
     */
    public Set<String> getDangerousCommandSet() {
        return Collections.unmodifiableSet(DANGEROUS_COMMANDS);
    }

    /**
     * 脱敏参数值（用于日志）
     */
    private String maskValue(String value) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= 10) {
            return value;
        }
        return value.substring(0, 5) + "***" + value.substring(value.length() - 2);
    }

    // ===== 参数白名单验证方法 =====

    /**
     * 使用白名单模式验证参数值
     * 
     * @param paramName     参数名
     * @param value         参数值
     * @param whitelistType 白名单类型（如 "alphanumeric", "filename", "ipv4" 等）
     * @throws SecurityException 如果参数不匹配白名单模式
     */
    public void validateWithWhitelist(String paramName, String value, String whitelistType) {
        if (value == null || value.isEmpty()) {
            return;
        }

        Pattern pattern = PARAM_WHITELIST_PATTERNS.get(whitelistType);
        if (pattern == null) {
            log.warn("Unknown whitelist type: {}, falling back to strict_alphanumeric", whitelistType);
            pattern = PARAM_WHITELIST_PATTERNS.get("strict_alphanumeric");
        }

        if (!pattern.matcher(value).matches()) {
            log.warn("Security: Parameter '{}' failed whitelist validation (type={}), value: {}",
                    paramName, whitelistType, maskValue(value));
            throw new SecurityException(
                    String.format("参数 '%s' 格式不符合要求（期望格式: %s）", paramName, whitelistType));
        }

        log.debug("Parameter '{}' passed whitelist validation (type={})", paramName, whitelistType);
    }

    /**
     * 检查参数是否包含禁止的特殊字符
     * 
     * @param paramName 参数名
     * @param value     参数值
     * @throws SecurityException 如果参数包含禁止的特殊字符
     */
    public void validateNoSpecialChars(String paramName, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        if (FORBIDDEN_SPECIAL_CHARS.matcher(value).find()) {
            log.warn("Security: Forbidden special character found in parameter '{}', value: {}",
                    paramName, maskValue(value));
            throw new SecurityException(
                    String.format("参数 '%s' 包含禁止的特殊字符", paramName));
        }
    }

    // ===== 路径遍历防护方法 =====

    /**
     * 路径遍历攻击模式
     */
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
            Pattern.compile("\\.\\.[\\/]"), // ../ or ..\
            Pattern.compile("[\\/]\\.\\."), // /.. or \..
            Pattern.compile("^\\.\\.[\\/]?"), // 开头的 ../ 或 ..
            Pattern.compile("%2e%2e[\\/]", Pattern.CASE_INSENSITIVE), // URL编码 ../
            Pattern.compile("%252e%252e", Pattern.CASE_INSENSITIVE), // 双重URL编码
            Pattern.compile("\\.\\.%c0%af"), // 畸形UTF-8编码 /
            Pattern.compile("\\.\\.%c1%9c"), // 畸形UTF-8编码 \
            Pattern.compile("%2e%2e%2f", Pattern.CASE_INSENSITIVE), // ../
            Pattern.compile("%2e%2e%5c", Pattern.CASE_INSENSITIVE), // ..\
            Pattern.compile("\\x2e\\x2e[\\/]"), // 十六进制编码
            Pattern.compile("\\u002e\\u002e[\\/]") // Unicode编码
    );

    /**
     * 敏感路径前缀（绝对禁止访问）
     * 注意：所有路径使用正斜杠，检查时会将反斜杠转换为正斜杠
     */
    private static final List<String> SENSITIVE_PATH_PREFIXES = Arrays.asList(
            // Linux/Unix 敏感路径
            "/etc/", "/root/", "/boot/", "/proc/", "/sys/",
            "/var/log/", "/var/run/", "/tmp/", "/dev/",
            "/.ssh/", "/.gnupg/", "/.bashrc", "/.bash_history",
            // 应用敏感路径
            "/config/", "/secrets/", "/credentials/",
            // Windows 敏感路径（使用正斜杠，检查时会转换）
            "c:/windows", "c:/system32", "c:/program files",
            "/windows", "/system32");

    /**
     * 验证路径安全性（防止路径遍历攻击）
     * 
     * @param paramName 参数名
     * @param path      路径值
     * @throws SecurityException 如果检测到路径遍历攻击
     */
    public void validatePathSecurity(String paramName, String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String normalizedPath = path.toLowerCase().replace("\\", "/");

        // 1. 检测路径遍历模式
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(path).find()) {
                log.warn("Security: Path traversal detected in parameter '{}', pattern: {}, path: {}",
                        paramName, pattern.pattern(), maskValue(path));
                throw new SecurityException(
                        String.format("参数 '%s' 包含路径遍历攻击模式，禁止使用 '..' 进行目录跳转", paramName));
            }
        }

        // 2. 检测敏感路径
        for (String sensitivePrefix : SENSITIVE_PATH_PREFIXES) {
            if (normalizedPath.startsWith(sensitivePrefix.toLowerCase()) ||
                    normalizedPath.contains(sensitivePrefix.toLowerCase())) {
                log.warn("Security: Sensitive path detected in parameter '{}', path: {}, matched: {}",
                        paramName, maskValue(path), sensitivePrefix);
                throw new SecurityException(
                        String.format("参数 '%s' 包含敏感系统路径，访问被拒绝", paramName));
            }
        }

        // 3. 检测绝对路径（可选，根据业务需求启用）
        if (path.startsWith("/") || (path.length() >= 2 && path.charAt(1) == ':')) {
            log.warn("Security: Absolute path detected in parameter '{}', path: {}",
                    paramName, maskValue(path));
            // 根据业务场景，可以选择拒绝或仅警告
            // throw new SecurityException(String.format("参数 '%s' 不允许使用绝对路径", paramName));
        }

        // 4. 路径规范化后再次检查
        String[] pathParts = normalizedPath.split("[\\/]+");
        int depth = 0;
        for (String part : pathParts) {
            if (part.equals("..")) {
                depth--;
                if (depth < 0) {
                    throw new SecurityException(
                            String.format("参数 '%s' 路径遍历超出根目录范围", paramName));
                }
            } else if (!part.isEmpty() && !part.equals(".")) {
                depth++;
            }
        }

        log.debug("Path security validation passed for parameter '{}'", paramName);
    }

    /**
     * 获取支持的白名单类型列表
     */
    public Set<String> getSupportedWhitelistTypes() {
        return Collections.unmodifiableSet(PARAM_WHITELIST_PATTERNS.keySet());
    }

    // ===== 命令注入综合防护 =====

    /**
     * 执行完整的命令安全校验（包含所有防护措施）
     * 
     * @param command    命令配置
     * @param params     参数映射
     * @param strictMode 是否启用严格模式（严格模式下会进行白名单验证）
     * @throws SecurityException 如果检测到安全威胁
     */
    public void performFullSecurityCheck(Command command, Map<String, Object> params, boolean strictMode) {
        if (params == null || params.isEmpty()) {
            return;
        }

        // 获取参数定义
        Map<String, ParamDefine> paramDefines = new HashMap<>();
        if (command.getCommandConfig() != null && command.getCommandConfig().getParams() != null) {
            for (ParamDefine def : command.getCommandConfig().getParams()) {
                paramDefines.put(def.getName(), def);
            }
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            String strValue = String.valueOf(value);

            // 1. 基础安全校验（命令注入检测）
            validateParamValue(paramName, strValue);

            // 2. 特殊字符检查
            if (strictMode) {
                validateNoSpecialChars(paramName, strValue);
            }

            // 3. 根据参数类型进行校验
            ParamDefine def = paramDefines.get(paramName);
            if (def != null) {
                String type = def.getType();
                if (type != null) {
                    // 类型校验
                    validateParamType(paramName, strValue, def);

                    // 路径类型额外进行路径遍历检查
                    if (type.equalsIgnoreCase("path") || type.equalsIgnoreCase("file")) {
                        validatePathSecurity(paramName, strValue);
                    }

                    // 严格模式下进行白名单验证
                    if (strictMode) {
                        String whitelistType = mapTypeToWhitelist(type);
                        if (whitelistType != null) {
                            validateWithWhitelist(paramName, strValue, whitelistType);
                        }
                    }
                }
            }
        }

        log.info("Full security check passed for {} parameters", params.size());
    }

    /**
     * 将参数类型映射到白名单类型
     */
    private String mapTypeToWhitelist(String type) {
        if (type == null) {
            return null;
        }
        switch (type.toLowerCase()) {
            case "integer":
            case "int":
                return "integer";
            case "number":
            case "float":
            case "double":
                return "number";
            case "boolean":
            case "bool":
                return "boolean";
            case "ip":
            case "ipaddress":
            case "ipv4":
                return "ipv4";
            case "hostname":
            case "host":
                return "hostname";
            case "port":
                return "port";
            case "url":
                return "url";
            case "email":
                return "email";
            case "filename":
                return "filename";
            case "path":
            case "file":
                return "safe_path";
            case "uuid":
                return "uuid";
            case "version":
                return "version";
            default:
                return null;
        }
    }
}
