package com.httprun.service.impl;

import com.httprun.entity.Token;
import com.httprun.exception.BusinessException;
import com.httprun.enums.ErrorCode;
import com.httprun.repository.TokenRepository;
import com.httprun.security.JwtTokenProvider;
import com.httprun.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Token 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public Token createToken(String name, String subject, boolean isAdmin, int expiresIn) {
        // 计算过期时间（Unix 时间戳，秒）
        long now = Instant.now().getEpochSecond();
        long expiresAt;
        if (expiresIn <= 0) {
            // 默认 1 年
            expiresAt = now + 365L * 24 * 60 * 60;
        } else {
            expiresAt = now + (long) expiresIn * 60 * 60;
        }

        // 生成 JWT
        String jwtToken = jwtTokenProvider.generateToken(subject, name, isAdmin, expiresAt);

        // 保存到数据库
        Token token = new Token();
        token.setName(name);
        token.setSubject(subject);
        token.setIsAdmin(isAdmin);
        token.setIssuedAt(now);
        token.setExpiresAt(expiresAt);
        token.setJwtToken(jwtToken);
        token.setRevoked(false);

        token = tokenRepository.save(token);

        log.info("Created token: id={}, name={}, isAdmin={}", token.getId(), name, isAdmin);

        return token;
    }

    @Override
    public Token getToken(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));
    }

    @Override
    public List<Token> listAllTokens() {
        return tokenRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Page<Token> listTokens(int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return tokenRepository.findAll(pageRequest);
    }

    @Override
    @Transactional
    public void revokeToken(Long id) {
        Token token = tokenRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));

        token.setRevoked(true);
        tokenRepository.save(token);

        log.info("Revoked token: id={}, name={}", token.getId(), token.getName());
    }

    @Override
    @Transactional
    public void deleteTokens(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        int count = tokenRepository.deleteByIdIn(ids);
        log.info("Deleted {} tokens", count);
    }

    @Override
    public boolean validateToken(String jwtToken) {
        // 先验证 JWT 签名
        if (!jwtTokenProvider.validateToken(jwtToken)) {
            return false;
        }

        // 再检查数据库中的状态
        long now = Instant.now().getEpochSecond();
        return tokenRepository.findValidToken(jwtToken, now).isPresent();
    }

    @Override
    public Token getTokenByJwt(String jwtToken) {
        return tokenRepository.findByJwtToken(jwtToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // 每小时执行一次
    public int cleanExpiredTokens() {
        long now = Instant.now().getEpochSecond();
        int count = tokenRepository.revokeExpiredTokens(now);
        if (count > 0) {
            log.info("Cleaned {} expired tokens", count);
        }
        return count;
    }
}
