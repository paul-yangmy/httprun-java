package com.httprun.service;

import com.httprun.dto.request.CreateCommandRequest;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.dto.response.CommandExecutionResult;
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
     * 更新命令状态
     */
    void updateCommandStatus(List<String> names, String status);

    /**
     * 删除命令
     */
    void deleteCommands(List<String> names);

    /**
     * 运行命令
     */
    CommandExecutionResult runCommand(RunCommandRequest request, String tokenSubject);
}
