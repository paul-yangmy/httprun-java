package com.httprun.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 审计上下文 - 用于传递请求的审计信息
 */
@Data
@Builder
public class AuditContext {

    /**
     * Token ID / 用户名
     */
    private String tokenId;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 请求方法 (GET/POST/PUT/DELETE)
     */
    private String method;

    /**
     * User-Agent 浏览器/客户端标识
     */
    private String userAgent;

    /**
     * Referer 请求来源页面
     */
    private String referer;

    /**
     * 请求来源类型 (WEB/API/CLI)
     */
    private String source;

    /**
     * X-Forwarded-For 原始IP链
     */
    private String forwardedFor;

    /**
     * 请求ID（用于链路追踪）
     */
    private String requestId;

    /**
     * 命令名称
     */
    private String commandName;

    /**
     * 请求内容
     */
    private String request;

    /**
     * 响应内容
     */
    private String response;

    /**
     * HTTP状态码
     */
    private Integer statusCode;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 根据 X-Source 请求头和 User-Agent 推断请求来源。
     * <p>
     * 优先级：
     * 1. X-Source: WEB  ——— Webapp 前端页面主动标识，直接采用（最可信）
     * 2. User-Agent 包含 curl/wget/httpie 等 CLI 关键字 ——— 命令行工具
     * 3. User-Agent 包含 httprun-cli ——— HttpRun CLI
     * 4. 其余情况 ——— API 调用（Postman、脚本、SDK 等）
     * <p>
     * 注意：不再以浏览器 UA（mozilla/chrome 等）判断 WEB，因为 Postman 等工具
     * 有时也会携带浏览器 UA，仅依赖 X-Source 标头保证准确性。
     */
    public static String inferSource(String userAgent, String xSource) {
        // 1. 优先信任前端显式标识
        if ("WEB".equalsIgnoreCase(xSource)) {
            return "WEB";
        }
        if (userAgent == null || userAgent.isEmpty()) {
            return "API";
        }
        // 2. CLI 工具检测
        String ua = userAgent.toLowerCase();
        if (ua.contains("curl") || ua.contains("wget") || ua.contains("httpie") || ua.contains("postman")) {
            return "CLI";
        }
        if (ua.contains("httprun-cli")) {
            return "CLI";
        }
        // 3. 其余均视为 API 调用（不再以浏览器 UA 判断 WEB）
        return "API";
    }

    /**
     * 向后兼容的单参数重载（仅传 User-Agent，不含 X-Source 时默认无标识）
     */
    public static String inferSource(String userAgent) {
        return inferSource(userAgent, null);
    }
}
