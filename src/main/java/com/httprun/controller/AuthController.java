package com.httprun.controller;

import com.httprun.entity.Token;
import com.httprun.service.JwtService;
import com.httprun.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Auth API", description = "认证接口 - 提供 Token 生成、验证和刷新功能")
public class AuthController {

    private final JwtService jwtService;
    private final TokenService tokenService;

    @PostMapping("/token")
    @Operation(summary = "创建访问 Token", description = "生成新的 JWT Token，用于 API 认证")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token 创建成功",
                    content = @Content(schema = @Schema(implementation = Token.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<Token> createToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Token 创建参数：name(名称), subject(授权范围), isAdmin(是否管理员), expiresIn(过期时间/小时)",
                    content = @Content(schema = @Schema(example = "{\"name\":\"my-token\",\"subject\":\"*\",\"isAdmin\":false,\"expiresIn\":24}")))
            @RequestBody Map<String, Object> request) {
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
    @Operation(summary = "验证 Token", description = "验证 JWT Token 的有效性，返回 Token 详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "验证结果返回",
                    content = @Content(schema = @Schema(example = "{\"valid\":true,\"name\":\"my-token\",\"subject\":\"*\",\"isAdmin\":false}"))),
            @ApiResponse(responseCode = "400", description = "Token 格式错误")
    })
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(description = "JWT Token", required = false)
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
    @Operation(summary = "刷新 Token", description = "使用现有 Token 生成新的 Token，延长使用期限")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token 刷新成功",
                    content = @Content(schema = @Schema(example = "{\"token\":\"new-jwt-token-here\"}"))),
            @ApiResponse(responseCode = "400", description = "Token 无效或已过期")
    })
    public ResponseEntity<Map<String, Object>> refreshToken(
            @Parameter(description = "JWT Token", required = true)
            @RequestHeader("x-token") String token) {
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }

        String newToken = jwtService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }
}
