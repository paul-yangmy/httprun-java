package com.httprun.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 提供者
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(String subject, String name, boolean isAdmin, long expiresAt) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name); // 用户名
        claims.put("admin", isAdmin); // 是否管理员

        return Jwts.builder()
                .subject(subject) // 授权的命令列表
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(expiresAt * 1000))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取授权命令列表
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 检查是否管理员
     */
    public boolean isAdmin(String token) {
        return parseToken(token).get("admin", Boolean.class);
    }

    /**
     * 获取用户名
     */
    public String getName(String token) {
        return parseToken(token).get("name", String.class);
    }
}
