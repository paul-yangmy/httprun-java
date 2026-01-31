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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Admin API", description = "管理员接口 - 用于管理命令、Token 和访问日志")
public class AdminController {

    private final CommandService commandService;
    private final TokenService tokenService;
    private final AccessLogService accessLogService;

    // ========== 命令管理 ==========

    @PostMapping("/command")
    @Operation(summary = "创建命令", description = "创建新的可执行命令，支持参数模板和权限控制")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "命令创建成功",
                    content = @Content(schema = @Schema(implementation = CommandResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
    })
    public ResponseEntity<CommandResponse> createCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令创建请求")
            @Valid @RequestBody CreateCommandRequest request) {
        return ResponseEntity.ok(commandService.createCommand(request));
    }

    @PutMapping("/command/{name}")
    @Operation(summary = "更新命令", description = "根据命令名称更新命令配置")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "命令更新成功"),
            @ApiResponse(responseCode = "404", description = "命令不存在")
    })
    public ResponseEntity<CommandResponse> updateCommand(
            @Parameter(description = "命令名称", example = "deploy-app")
            @PathVariable String name,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令更新内容")
            @Valid @RequestBody CreateCommandRequest request) {
        return ResponseEntity.ok(commandService.updateCommand(name, request));
    }

    @GetMapping("/commands")
    @Operation(summary = "获取所有命令", description = "返回系统中所有已配置的命令列表")
    @ApiResponse(responseCode = "200", description = "命令列表获取成功")
    public ResponseEntity<List<CommandResponse>> getCommandList() {
        return ResponseEntity.ok(commandService.listAllCommands());
    }

    @PutMapping("/commands")
    @Operation(summary = "更新命令状态", description = "批量启用或禁用命令")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<Map<String, Boolean>> updateCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令状态更新请求")
            @Valid @RequestBody UpdateCommandRequest request) {
        commandService.updateCommandStatus(request.getCommands(), request.getStatus());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/commands")
    @Operation(summary = "删除命令", description = "根据命令名称删除一个或多个命令，名称之间用空格分隔")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "命令删除成功"),
            @ApiResponse(responseCode = "404", description = "命令不存在")
    })
    public ResponseEntity<Void> deleteCommand(
            @Parameter(description = "命令名称，多个名称用空格分隔", example = "deploy-app backup-db")
            @RequestParam String name) {
        commandService.deleteCommands(List.of(name.split(" ")));
        return ResponseEntity.ok().build();
    }

    // ========== Token 管理 ==========

    @GetMapping("/tokens")
    @Operation(summary = "获取 Token 列表", description = "返回所有已创建的访问 Token")
    @ApiResponse(responseCode = "200", description = "Token 列表获取成功")
    public ResponseEntity<List<Token>> getTokenList() {
        return ResponseEntity.ok(tokenService.listAllTokens());
    }

    @PostMapping("/token")
    @Operation(summary = "创建 Token", description = "创建新的访问 Token，可指定权限范围和过期时间")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token 创建成功",
                    content = @Content(schema = @Schema(implementation = Token.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<Token> createToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Token 创建请求")
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
    @Operation(summary = "删除/撤销 Token", description = "撤销指定的访问 Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token 撤销成功"),
            @ApiResponse(responseCode = "404", description = "Token 不存在")
    })
    public ResponseEntity<Void> deleteToken(
            @Parameter(description = "Token ID", example = "1")
            @PathVariable Long tokenId) {
        tokenService.revokeToken(tokenId);
        return ResponseEntity.ok().build();
    }

    // ========== 访问日志 ==========

    @GetMapping("/accesslog")
    @Operation(summary = "获取访问日志", description = "分页查询系统访问日志，包含命令执行记录和 API 调用历史")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "日志获取成功"),
            @ApiResponse(responseCode = "400", description = "分页参数错误")
    })
    public ResponseEntity<Page<AccessLog>> getAccessLogList(
            @Parameter(description = "页码（从 1 开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数", example = "10")
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(accessLogService.listLogs(page, pageSize));
    }
}
