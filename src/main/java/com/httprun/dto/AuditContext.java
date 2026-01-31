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
     * 根据User-Agent推断请求来源
     */
    public static String inferSource(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "API";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("edge")) {
            return "WEB";
        }
        if (ua.contains("curl") || ua.contains("wget") || ua.contains("httpie") || ua.contains("postman")) {
            return "CLI";
        }
        if (ua.contains("httprun-cli")) {
            return "CLI";
        }
        return "API";
    }
}
