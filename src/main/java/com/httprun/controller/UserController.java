package com.httprun.controller;

import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.security.JwtUserPrincipal;
import com.httprun.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "User API", description = "用户命令执行接口")
public class UserController {

    private final CommandService commandService;

    @GetMapping("/commands")
    @Operation(summary = "获取用户可执行的命令列表")
    public ResponseEntity<List<CommandResponse>> getCommandList(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        if (principal.admin()) {
            return ResponseEntity.ok(commandService.listAllCommands());
        }

        List<String> allowedCommands = Arrays.asList(principal.subject().split(","));
        return ResponseEntity.ok(commandService.listCommands(allowedCommands));
    }

    @GetMapping("/valid")
    @Operation(summary = "验证 Token 有效性")
    public ResponseEntity<Map<String, Boolean>> validateToken() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/**")
    @Operation(summary = "执行命令")
    public ResponseEntity<CommandExecutionResult> runCommand(
            @RequestBody RunCommandRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        String subject = principal.admin() ? "admin" : principal.subject();
        CommandExecutionResult result = commandService.runCommand(request, subject);
        return ResponseEntity.ok(result);
    }
}
