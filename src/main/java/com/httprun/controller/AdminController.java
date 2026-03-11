package com.httprun.controller;

import com.httprun.dto.request.CommandImportRequest;
import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.CreateTokenRequest;
import com.httprun.dto.request.UpdateCommandRequest;
import com.httprun.dto.response.CommandImportResult;
import com.httprun.dto.response.CommandResponse;
import com.httprun.dto.response.CommandVersionResponse;
import com.httprun.dto.response.RevokeTokenResponse;
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
                        @ApiResponse(responseCode = "200", description = "命令创建成功", content = @Content(schema = @Schema(implementation = CommandResponse.class))),
                        @ApiResponse(responseCode = "400", description = "请求参数错误"),
                        @ApiResponse(responseCode = "401", description = "未授权访问")
        })
        public ResponseEntity<CommandResponse> createCommand(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令创建请求") @Valid @RequestBody CreateCommandRequest request) {
                return ResponseEntity.ok(commandService.createCommand(request));
        }

        @PutMapping("/command/{name}")
        @Operation(summary = "更新命令", description = "根据命令名称更新命令配置")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "命令更新成功"),
                        @ApiResponse(responseCode = "404", description = "命令不存在")
        })
        public ResponseEntity<CommandResponse> updateCommand(
                        @Parameter(description = "命令名称", example = "deploy-app") @PathVariable String name,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令更新内容") @Valid @RequestBody CreateCommandRequest request) {
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
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "命令状态更新请求") @Valid @RequestBody UpdateCommandRequest request) {
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
                        @Parameter(description = "命令名称，多个名称用空格分隔", example = "deploy-app backup-db") @RequestParam String name) {
                commandService.deleteCommands(List.of(name.split(" ")));
                return ResponseEntity.ok().build();
        }

        @GetMapping("/commands/export")
        @Operation(summary = "导出命令", description = "将指定命令（或全部命令）导出为 JSON 格式，便于在不同环境间同步。SSH 密码/私钥字段不导出，需在目标系统重新配置")
        @ApiResponse(responseCode = "200", description = "导出成功")
        public ResponseEntity<List<CreateCommandRequest>> exportCommands(
                        @Parameter(description = "命令名称列表（逗号分隔），空表示导出全部", example = "deploy-app,backup-db") @RequestParam(required = false) String names) {
                List<String> nameList = (names != null && !names.isBlank())
                                ? List.of(names.split(","))
                                : List.of();
                return ResponseEntity.ok(commandService.exportCommands(nameList));
        }

        @PostMapping("/commands/import")
        @Operation(summary = "导入命令", description = "批量导入命令配置（JSON 列表）。mode=skip 时跳过同名命令，mode=overwrite 时覆盖同名命令")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "导入完成，返回各状态计数"),
                        @ApiResponse(responseCode = "400", description = "请求格式错误")
        })
        public ResponseEntity<CommandImportResult> importCommands(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "导入请求") @jakarta.validation.Valid @RequestBody CommandImportRequest request) {
                return ResponseEntity.ok(commandService.importCommands(request));
        }

        @GetMapping("/commands/groups")
        @Operation(summary = "获取所有命令分组名称", description = "返回所有命令的 groupName 去重列表，供前端分组选择使用")
        @ApiResponse(responseCode = "200", description = "分组列表获取成功")
        public ResponseEntity<List<String>> getCommandGroups() {
                return ResponseEntity.ok(commandService.listAllGroupNames());
        }

        @GetMapping("/command/{name}/versions")
        @Operation(summary = "获取命令版本历史", description = "返回指定命令的所有版本历史，按版本号倒序排列")
        @ApiResponse(responseCode = "200", description = "版本历史获取成功")
        public ResponseEntity<List<CommandVersionResponse>> getCommandVersions(
                        @Parameter(description = "命令名称") @PathVariable String name) {
                return ResponseEntity.ok(commandService.listCommandVersions(name));
        }

        @PostMapping("/command/{name}/rollback/{versionId}")
        @Operation(summary = "回滚命令到指定版本", description = "将命令回滚到历史版本的配置状态，回滚操作本身也会产生一条新的版本记录")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "回滚成功"),
                        @ApiResponse(responseCode = "404", description = "命令或版本不存在")
        })
        public ResponseEntity<CommandResponse> rollbackCommandVersion(
                        @Parameter(description = "命令名称") @PathVariable String name,
                        @Parameter(description = "版本 ID") @PathVariable Long versionId) {
                return ResponseEntity.ok(commandService.rollbackCommandVersion(name, versionId));
        }

        // ========== Token 管理 ==========

        @GetMapping("/tokens")
        @Operation(summary = "获取 Token 列表", description = "返回所有已创建的访问 Token")
        @ApiResponse(responseCode = "200", description = "Token 列表获取成功")
        public ResponseEntity<List<Token>> getTokenList() {
                return ResponseEntity.ok(tokenService.listAllTokens());
        }

        @PostMapping("/token")
        @Operation(summary = "创建 Token", description = "创建新的访问 Token，可指定权限范围、过期时间和时间范围限制")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Token 创建成功", content = @Content(schema = @Schema(implementation = Token.class))),
                        @ApiResponse(responseCode = "400", description = "请求参数错误")
        })
        public ResponseEntity<Token> createToken(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Token 创建请求") @Valid @RequestBody CreateTokenRequest request) {
                Token token = tokenService.createToken(request);
                return ResponseEntity.ok(token);
        }

        @DeleteMapping("/token/{tokenId}")
        @Operation(summary = "删除/撤销 Token", description = "撤销指定的访问 Token。如果撤销的是管理员 Token，系统会自动生成新的管理员 Token 并返回")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Token 撤销成功", content = @Content(schema = @Schema(implementation = RevokeTokenResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Token 不存在")
        })
        public ResponseEntity<RevokeTokenResponse> deleteToken(
                        @Parameter(description = "Token ID", example = "1") @PathVariable Long tokenId) {
                RevokeTokenResponse response = tokenService.revokeToken(tokenId);
                return ResponseEntity.ok(response);
        }

        // ========== 访问日志 ==========

        @GetMapping("/accesslog")
        @Operation(summary = "获取访问日志", description = "分页查询系统访问日志，支持按类型筛选")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "日志获取成功"),
                        @ApiResponse(responseCode = "400", description = "分页参数错误")
        })
        public ResponseEntity<Page<AccessLog>> getAccessLogList(
                        @Parameter(description = "页码（从 1 开始）", example = "1") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
                        @Parameter(description = "日志类型筛选：command（仅命令执行）/all（全部）", example = "all") @RequestParam(defaultValue = "all") String type,
                        @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
                // 根据 type 参数决定是否只显示命令执行记录
                boolean commandOnly = "command".equalsIgnoreCase(type);
                return ResponseEntity.ok(accessLogService.searchLogs(
                                null, null, null, null, null, keyword, commandOnly, page, pageSize));
        }
}
