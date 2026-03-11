package com.httprun.service;

import com.httprun.dto.request.CommandImportRequest;
import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
import com.httprun.dto.response.CommandImportResult;
import com.httprun.dto.response.CommandResponse;

import java.util.List;

/**
 * 命令服务接口
 */
public interface CommandService {

    /**
     * 创建命令
     */
    CommandResponse createCommand(CreateCommandRequest request);

    /**
     * 更新命令
     */
    CommandResponse updateCommand(String name, CreateCommandRequest request);

    /**
     * 获取所有命令
     */
    List<CommandResponse> listAllCommands();

    /**
     * 根据名称列表获取命令
     */
    List<CommandResponse> listCommands(List<String> names);

    /**
     * 根据分组列表获取命令
     */
    List<CommandResponse> listCommandsByGroups(List<String> groups);

    /**
     * 获取所有命令分组名称
     */
    List<String> listAllGroupNames();

    /**
     * 更新命令状态
     */
    void updateCommandStatus(List<String> names, String status);

    /**
     * 删除命令
     */
    void deleteCommands(List<String> names);

    /**
     * 运行命令
     *
     * @param request       执行请求
     * @param tokenSubject  Token subject（命令名列表或 "admin"）
     * @param allowedGroups Token 允许的分组范围（逗号分隔，null 表示不限制）
     */
    CommandExecutionResult runCommand(RunCommandRequest request, String tokenSubject, String allowedGroups);

    /**
     * 导出命令（JSON 格式，敏感字段脱敏）
     *
     * @param names 命令名称列表，空/null 表示导出全部
     */
    List<CreateCommandRequest> exportCommands(List<String> names);

    /**
     * 导入命令
     */
    CommandImportResult importCommands(CommandImportRequest request);

    /**
     * 获取指定命令的版本历史列表（倒序）
     */
    List<com.httprun.dto.response.CommandVersionResponse> listCommandVersions(String commandName);

    /**
     * 回滚命令到指定版本
     *
     * @param commandName 命令名称
     * @param versionId   版本记录 ID
     */
    com.httprun.dto.response.CommandResponse rollbackCommandVersion(String commandName, Long versionId);
}
