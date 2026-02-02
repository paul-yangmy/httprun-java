package com.httprun.security;

import com.httprun.entity.Token;
import com.httprun.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "x-token";
    private static final String TOKEN_PARAM = "token";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 优先从 header 获取 token，其次从 query parameter 获取
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            token = request.getParameter(TOKEN_PARAM);
        }

        if (token != null && !token.isEmpty()) {
            try {
                // 1. 验证 JWT 签名和有效期
                if (!jwtTokenProvider.validateToken(token)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid token");
                    return;
                }

                // 2. 验证 Token 是否在数据库中存在且未被撤销
                String name = jwtTokenProvider.getName(token);
                Optional<Token> tokenEntityOpt = tokenRepository.findByJwtToken(token);
                if (tokenEntityOpt.isEmpty() || tokenEntityOpt.get().getRevoked()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token not found or revoked");
                    return;
                }

                Token tokenEntity = tokenEntityOpt.get();

                // 3. 验证时间范围权限（仅对非管理员接口进行检查）
                String path = request.getRequestURI();
                if (path.startsWith("/api/run") && !tokenEntity.isWithinAllowedTimeRange()) {
                    log.warn("Token {} is outside allowed time range", name);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Token is outside allowed time range. Check allowed hours and weekdays.");
                    return;
                }

                // 4. 构建认证信息
                boolean isAdmin = jwtTokenProvider.isAdmin(token);
                String subject = jwtTokenProvider.getSubject(token);

                var authorities = isAdmin ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                var authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(name, subject, isAdmin),
                        token,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                log.error("JWT authentication failed", e);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/api/health");
    }
}
