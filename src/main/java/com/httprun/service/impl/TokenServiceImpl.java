package com.httprun.service.impl;

import com.httprun.dto.request.CreateTokenRequest;
import com.httprun.dto.response.RevokeTokenResponse;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
        // 管理员唯一性检查：如果要创建管理员 Token，检查是否已存在有效的管理员
        if (isAdmin) {
            long now = Instant.now().getEpochSecond();
            if (tokenRepository.existsValidAdminToken(now)) {
                log.warn("试图创建管理员 Token，但已存在有效的管理员 Token");
                throw new BusinessException(ErrorCode.ADMIN_ALREADY_EXISTS);
            }
        }

        // 计算过期时间（Unix 时间戳，秒）
        long now = Instant.now().getEpochSecond();
        Long expiresAt;
        if (expiresIn == -1) {
            // -1 表示永久有效
            expiresAt = null;
        } else if (expiresIn <= 0) {
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
    @Transactional
    public Token createToken(CreateTokenRequest request) {
        // 计算过期时间（Unix 时间戳，秒）
        long now = Instant.now().getEpochSecond();
        
        boolean isAdmin = request.getIsAdmin() != null && request.getIsAdmin();
        
        // 管理员唯一性检查：如果要创建管理员 Token，检查是否已存在有效的管理员
        if (isAdmin) {
            if (tokenRepository.existsValidAdminToken(now)) {
                log.warn("试图创建管理员 Token，但已存在有效的管理员 Token");
                throw new BusinessException(ErrorCode.ADMIN_ALREADY_EXISTS);
            }
        }
        
        int expiresIn = request.getExpiresIn() != null ? request.getExpiresIn() : 24;
        Long expiresAt;
        if (expiresIn == -1) {
            // -1 表示永久有效
            expiresAt = null;
        } else if (expiresIn <= 0) {
            // 默认 1 年
            expiresAt = now + 365L * 24 * 60 * 60;
        } else {
            expiresAt = now + (long) expiresIn * 60 * 60;
        }

        // 处理授权命令
        String subject = request.getCommands() != null && !request.getCommands().isEmpty()
                ? String.join(",", request.getCommands())
                : "*";

        // 生成 JWT
        String jwtToken = jwtTokenProvider.generateToken(subject, request.getName(), isAdmin, expiresAt);

        // 保存到数据库
        Token token = new Token();
        token.setName(request.getName());
        token.setSubject(subject);
        token.setIsAdmin(isAdmin);
        token.setIssuedAt(now);
        token.setExpiresAt(expiresAt);
        token.setJwtToken(jwtToken);
        token.setRevoked(false);

        // 设置时间范围限制
        token.setAllowedStartTime(request.getAllowedStartTime());
        token.setAllowedEndTime(request.getAllowedEndTime());

        // 处理允许的星期几
        if (request.getAllowedWeekdays() != null && !request.getAllowedWeekdays().isEmpty()) {
            String weekdays = request.getAllowedWeekdays().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            token.setAllowedWeekdays(weekdays);
        }

        token.setRemark(request.getRemark());

        token = tokenRepository.save(token);

        log.info(
                "Created token with time restrictions: id={}, name={}, isAdmin={}, startTime={}, endTime={}, weekdays={}",
                token.getId(), request.getName(), isAdmin,
                token.getAllowedStartTime(), token.getAllowedEndTime(), token.getAllowedWeekdays());

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
    public RevokeTokenResponse revokeToken(Long id) {
        Token token = tokenRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));

        boolean wasAdmin = Boolean.TRUE.equals(token.getIsAdmin());
        
        // 撤销当前 Token
        token.setRevoked(true);
        tokenRepository.save(token);

        log.info("Revoked token: id={}, name={}, wasAdmin={}", token.getId(), token.getName(), wasAdmin);

        // 如果撤销的是管理员 Token，自动生成新的管理员 Token
        if (wasAdmin) {
            log.warn("Admin token revoked, generating new admin token automatically");
            
            // 构建新的管理员 Token 请求
            CreateTokenRequest adminRequest = new CreateTokenRequest();
            adminRequest.setName("admin");
            adminRequest.setCommands(null); // 管理员拥有所有命令权限
            adminRequest.setIsAdmin(true);
            adminRequest.setExpiresIn(0); // 默认 1 年
            adminRequest.setRemark("系统自动生成的管理员 Token（原管理员 Token 被撤销）");
            adminRequest.setAllowedStartTime(null);
            adminRequest.setAllowedEndTime(null);
            adminRequest.setAllowedWeekdays(null);

            // 创建新的管理员 Token（跳过唯一性检查，因为旧的已经撤销）
            Token newAdminToken = createAdminTokenInternal(adminRequest);
            
            log.info("New admin token generated: id={}, name={}", newAdminToken.getId(), newAdminToken.getName());
            
            return RevokeTokenResponse.adminRevoke(
                    newAdminToken.getId(),
                    newAdminToken.getName(),
                    newAdminToken.getJwtToken()
            );
        }

        return RevokeTokenResponse.normalRevoke();
    }

    /**
     * 内部方法：创建管理员 Token（跳过唯一性检查）
     * 仅在撤销管理员 Token 后自动生成新 Token 时使用
     */
    private Token createAdminTokenInternal(CreateTokenRequest request) {
        long now = Instant.now().getEpochSecond();
        int expiresIn = request.getExpiresIn() != null ? request.getExpiresIn() : 24;
        Long expiresAt;
        if (expiresIn == -1) {
            expiresAt = null;
        } else if (expiresIn <= 0) {
            expiresAt = now + 365L * 24 * 60 * 60;
        } else {
            expiresAt = now + (long) expiresIn * 60 * 60;
        }

        String subject = "*"; // 管理员拥有所有权限
        boolean isAdmin = true;

        String jwtToken = jwtTokenProvider.generateToken(subject, request.getName(), isAdmin, expiresAt);

        Token token = new Token();
        token.setName(request.getName());
        token.setSubject(subject);
        token.setIsAdmin(isAdmin);
        token.setIssuedAt(now);
        token.setExpiresAt(expiresAt);
        token.setJwtToken(jwtToken);
        token.setRevoked(false);
        token.setRemark(request.getRemark());

        return tokenRepository.save(token);
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
    public boolean validateTokenTimeRange(String jwtToken) {
        Optional<Token> tokenOpt = tokenRepository.findByJwtToken(jwtToken);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        Token token = tokenOpt.get();
        return token.isWithinAllowedTimeRange();
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
