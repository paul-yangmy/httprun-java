package com.httprun.service.impl;

import com.httprun.config.JwtConfig;
import com.httprun.security.JwtTokenProvider;
import com.httprun.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * JWT 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;

    @Override
    public String generateToken(String subject, String name, boolean isAdmin, long expiresAt) {
        return jwtTokenProvider.generateToken(subject, name, isAdmin, expiresAt);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public String getSubject(String token) {
        return jwtTokenProvider.getSubject(token);
    }

    @Override
    public String getName(String token) {
        return jwtTokenProvider.getName(token);
    }

    @Override
    public boolean isAdmin(String token) {
        return jwtTokenProvider.isAdmin(token);
    }

    @Override
    public String refreshToken(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        String subject = getSubject(token);
        String name = getName(token);
        boolean isAdmin = isAdmin(token);

        // 新的过期时间
        long expiresAt = Instant.now().getEpochSecond() + (jwtConfig.getExpiration() / 1000);

        return generateToken(subject, name, isAdmin, expiresAt);
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            return !validateToken(token);
        } catch (Exception e) {
            return true;
        }
    }
}
