package com.httprun.service;

/**
 * JWT 服务接口
 */
public interface JwtService {

    /**
     * 生成 Token
     *
     * @param subject   授权主体（命令列表）
     * @param name      Token 名称
     * @param isAdmin   是否管理员
     * @param expiresAt 过期时间（Unix 时间戳，秒）
     * @return JWT Token
     */
    String generateToken(String subject, String name, boolean isAdmin, long expiresAt);

    /**
     * 验证 Token
     */
    boolean validateToken(String token);

    /**
     * 获取 Subject
     */
    String getSubject(String token);

    /**
     * 获取用户名
     */
    String getName(String token);

    /**
     * 检查是否管理员
     */
    boolean isAdmin(String token);

    /**
     * 刷新 Token
     */
    String refreshToken(String token);

    /**
     * 检查 Token 是否过期
     */
    boolean isTokenExpired(String token);
}
