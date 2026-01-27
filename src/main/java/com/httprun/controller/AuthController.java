package com.httprun.controller;

import com.httprun.entity.Token;
import com.httprun.service.JwtService;
import com.httprun.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "认证接口")
public class AuthController {

    private final JwtService jwtService;
    private final TokenService tokenService;

    @PostMapping("/token")
    @Operation(summary = "创建访问 Token")
    public ResponseEntity<Token> createToken(@RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("name", "api-token");
        String subject = (String) request.getOrDefault("subject", "*");
        boolean isAdmin = Boolean.TRUE.equals(request.get("isAdmin"));
        int expiresIn = request.containsKey("expiresIn")
                ? ((Number) request.get("expiresIn")).intValue()
                : 24;

        Token token = tokenService.createToken(name, subject, isAdmin, expiresIn);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/validate")
    @Operation(summary = "验证 Token")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "x-token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Token not provided"));
        }

        boolean valid = jwtService.validateToken(token);
        if (valid) {
            String subject = jwtService.getSubject(token);
            String name = jwtService.getName(token);
            boolean isAdmin = jwtService.isAdmin(token);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "name", name,
                    "subject", subject,
                    "isAdmin", isAdmin));
        } else {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Invalid token"));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader("x-token") String token) {
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }

        String newToken = jwtService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }
}
