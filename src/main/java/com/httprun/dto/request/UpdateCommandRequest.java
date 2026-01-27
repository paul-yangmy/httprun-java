package com.httprun.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 更新命令请求
 */
@Data
public class UpdateCommandRequest {

    /**
     * 要更新的命令名称列表
     */
    private List<String> commands;

    /**
     * 新状态
     */
    private String status;
}
