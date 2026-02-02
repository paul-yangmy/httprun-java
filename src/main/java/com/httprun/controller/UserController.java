package com.httprun.controller;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器（命令执行接口）
 */
@RestController
@RequestMapping("/api/run")
@RequiredArgsConstructor
@Tag(name = "User API", description = "用户命令执行接口 - 提供命令查询和执行功能")
public class UserController {

    private final CommandService commandService;

    @GetMapping("/commands")
    @Operation(summary = "获取用户可执行的命令列表", description = "根据当前用户权限返回可执行的命令列表。管理员可查看所有命令，普通用户仅看到授权的命令")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "命令列表获取成功", content = @Content(schema = @Schema(implementation = CommandResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权访问")
    })
    public ResponseEntity<List<CommandResponse>> getCommandList(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        if (principal.admin()) {
            return ResponseEntity.ok(commandService.listAllCommands());
        }

        List<String> allowedCommands = Arrays.asList(principal.subject().split(","));
        return ResponseEntity.ok(commandService.listCommands(allowedCommands));
    }

    @GetMapping("/valid")
    @Operation(summary = "验证 Token 有效性", description = "快速验证当前 Token 是否有效，用于健康检查")
    @ApiResponse(responseCode = "200", description = "Token 有效", content = @Content(schema = @Schema(example = "{\"ok\":true}")))
    public ResponseEntity<Map<String, Boolean>> validateToken() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/**")
    @Operation(summary = "执行命令", description = "执行指定的命令。命令名称从 URL 路径中获取，参数通过请求体传递。" +
            "系统会验证用户权限、参数安全性，并记录执行日志")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "命令执行成功", content = @Content(schema = @Schema(implementation = CommandExecutionResult.class))),
            @ApiResponse(responseCode = "400", description = "参数验证失败或命令不存在"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "命令执行失败")
    })
    public ResponseEntity<CommandExecutionResult> runCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令执行请求，包含命令名称和参数列表", content = @Content(schema = @Schema(example = "{\"name\":\"deploy-app\",\"params\":[{\"name\":\"env\",\"value\":\"prod\"},{\"name\":\"version\",\"value\":\"1.0.0\"}]}"))) @RequestBody RunCommandRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        String subject = principal.admin() ? "admin" : principal.subject();
        CommandExecutionResult result = commandService.runCommand(request, subject);
        return ResponseEntity.ok(result);
    }
}
