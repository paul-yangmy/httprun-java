package com.httprun.security;

/**
 * JWT 用户主体
 */
public record JwtUserPrincipal(String name, String subject, boolean admin) {
}
