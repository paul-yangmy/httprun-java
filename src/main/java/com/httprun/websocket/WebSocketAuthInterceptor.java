package com.httprun.websocket;

import com.httprun.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 认证拦截器
 * 在 WebSocket 握手时验证 JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket handshake rejected: Invalid token");
                return false;
            }

            // 将用户信息存入 attributes，供 Handler 使用
            String subject = jwtTokenProvider.getSubject(token);
            boolean isAdmin = jwtTokenProvider.isAdmin(token);
            String name = jwtTokenProvider.getName(token);

            attributes.put("subject", subject);
            attributes.put("isAdmin", isAdmin);
            attributes.put("name", name);
            attributes.put("token", token);

            log.debug("WebSocket handshake accepted for user: {}", name);
            return true;
        } catch (Exception e) {
            log.error("WebSocket handshake error", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理（可选）
    }

    /**
     * 从请求中提取 Token
     * 支持 query 参数: ?token=xxx
     */
    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null) {
                return token;
            }
        }

        // 也支持从 header 获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
