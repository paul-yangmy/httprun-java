package com.httprun.controller;

import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.CreateTokenRequest;
import com.httprun.dto.request.UpdateCommandRequest;
import com.httprun.dto.response.CommandResponse;
import com.httprun.entity.AccessLog;
import com.httprun.entity.Command;
import com.httprun.entity.Token;
import com.httprun.service.AccessLogService;
import com.httprun.service.CommandService;
import com.httprun.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API", description = "管理员接口")
public class AdminController {

    private final CommandService commandService;
    private final TokenService tokenService;
    private final AccessLogService accessLogService;

    // ========== 命令管理 ==========

    @PostMapping("/command")
    @Operation(summary = "创建命令")
    public ResponseEntity<CommandResponse> createCommand(
            @Valid @RequestBody CreateCommandRequest request) {
        return ResponseEntity.ok(commandService.createCommand(request));
    }

    @GetMapping("/commands")
    @Operation(summary = "获取所有命令")
    public ResponseEntity<List<CommandResponse>> getCommandList() {
        return ResponseEntity.ok(commandService.listAllCommands());
    }

    @PutMapping("/commands")
    @Operation(summary = "更新命令状态")
    public ResponseEntity<Map<String, Boolean>> updateCommand(
            @Valid @RequestBody UpdateCommandRequest request) {
        commandService.updateCommandStatus(request.getCommands(), request.getStatus());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/commands")
    @Operation(summary = "删除命令")
    public ResponseEntity<Void> deleteCommand(@RequestParam String name) {
        commandService.deleteCommands(List.of(name.split(" ")));
        return ResponseEntity.ok().build();
    }

    // ========== Token 管理 ==========

    @GetMapping("/tokens")
    @Operation(summary = "获取 Token 列表")
    public ResponseEntity<List<Token>> getTokenList() {
        return ResponseEntity.ok(tokenService.listAllTokens());
    }

    @PostMapping("/token")
    @Operation(summary = "创建 Token")
    public ResponseEntity<Token> createToken(
            @Valid @RequestBody CreateTokenRequest request) {
        String subject = request.getCommands() != null && !request.getCommands().isEmpty()
                ? String.join(",", request.getCommands())
                : "*";
        Token token = tokenService.createToken(
                request.getName(),
                subject,
                request.getIsAdmin() != null && request.getIsAdmin(),
                request.getExpiresIn() != null ? request.getExpiresIn() : 24);
        return ResponseEntity.ok(token);
    }

    @DeleteMapping("/token/{tokenId}")
    @Operation(summary = "删除/撤销 Token")
    public ResponseEntity<Void> deleteToken(@PathVariable Long tokenId) {
        tokenService.revokeToken(tokenId);
        return ResponseEntity.ok().build();
    }

    // ========== 访问日志 ==========

    @GetMapping("/accesslog")
    @Operation(summary = "获取访问日志")
    public ResponseEntity<Page<AccessLog>> getAccessLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(accessLogService.listLogs(page, pageSize));
    }
}
