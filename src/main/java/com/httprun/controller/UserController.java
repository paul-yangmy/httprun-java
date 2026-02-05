package com.httprun.controller;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.entity.AccessLog;
import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.AccessLogService;
import com.httprun.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final AccessLogService accessLogService;

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

    @GetMapping("/user")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息，包括用户名和管理员权限")
    @ApiResponse(responseCode = "200", description = "用户信息获取成功")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "name", principal.name(),
                "isAdmin", principal.admin(),
                "token", principal.subject()));
    }

    @GetMapping("/history")
    @Operation(summary = "获取执行历史", description = "获取命令执行历史记录。管理员可查看所有记录，普通用户仅能查看自己的记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "执行历史获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
    })
    public ResponseEntity<Page<AccessLog>> getExecutionHistory(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "页码（从 1 开始）", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数", example = "20") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "Token 名称筛选（仅管理员可用）") @RequestParam(required = false) String tokenName,
            @Parameter(description = "命令名称筛选") @RequestParam(required = false) String commandName,
            @Parameter(description = "状态筛选：success/error") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间（ISO 格式）") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间（ISO 格式）") @RequestParam(required = false) String endTime,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {

        // 解析时间参数
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startTime != null && !startTime.isEmpty()) {
            start = java.time.LocalDateTime.parse(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            end = java.time.LocalDateTime.parse(endTime);
        }

        // 确定查询的 tokenId
        String queryTokenId;
        if (principal.admin()) {
            // 管理员可以查看所有记录，或根据 tokenName 筛选
            queryTokenId = (tokenName != null && !tokenName.isEmpty()) ? tokenName : null;
        } else {
            // 普通用户只能查看自己的记录，忽略 tokenName 参数
            queryTokenId = principal.name();
        }

        // 执行历史只显示命令执行记录（commandOnly=true）
        return ResponseEntity.ok(accessLogService.searchLogs(
                queryTokenId, commandName, status, start, end, keyword, true, page, pageSize));
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除执行记录", description = "删除指定的执行记录。普通用户只能删除自己的记录，管理员可删除任意记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "无权删除此记录"),
            @ApiResponse(responseCode = "404", description = "记录不存在")
    })
    public ResponseEntity<Map<String, Object>> deleteExecutionHistory(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "记录 ID") @PathVariable Long id) {

        AccessLog log = accessLogService.getLogById(id);
        if (log == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "记录不存在"));
        }

        // 权限检查：普通用户只能删除自己的记录
        if (!principal.admin() && !principal.name().equals(log.getTokenId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权删除此记录"));
        }

        accessLogService.deleteLog(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
    }

    @DeleteMapping("/history")
    @Operation(summary = "批量删除执行记录", description = "批量删除执行记录。普通用户只能删除自己的记录，管理员可删除任意记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "无权删除部分记录")
    })
    public ResponseEntity<Map<String, Object>> deleteExecutionHistoryBatch(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "记录 ID 列表（逗号分隔）") @RequestParam String ids) {

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();

        if (idList.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "请提供要删除的记录 ID"));
        }

        // 非管理员需要检查每条记录的所有权
        if (!principal.admin()) {
            for (Long id : idList) {
                AccessLog log = accessLogService.getLogById(id);
                if (log != null && !principal.name().equals(log.getTokenId())) {
                    return ResponseEntity.status(403).body(Map.of(
                            "success", false,
                            "message", "无权删除 ID=" + id + " 的记录"));
                }
            }
        }

        int deleted = accessLogService.deleteLogs(idList);
        return ResponseEntity.ok(Map.of("success", true, "message", "成功删除 " + deleted + " 条记录", "deleted", deleted));
    }

    @DeleteMapping("/history/clear")
    @Operation(summary = "清空执行记录", description = "清空当前用户的所有执行记录。管理员清空所有记录需使用管理接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "清空成功")
    })
    public ResponseEntity<Map<String, Object>> clearExecutionHistory(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        int deleted = accessLogService.deleteLogsByToken(principal.name());
        return ResponseEntity.ok(Map.of("success", true, "message", "成功清空 " + deleted + " 条记录", "deleted", deleted));
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
