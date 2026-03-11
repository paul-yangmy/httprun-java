package com.httprun.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 命令版本历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandVersionResponse {

    private Long id;

    private String commandName;

    private Integer version;

    /**
     * 命令配置快照（JSON 字符串，前端可自行解析展示）
     */
    private String snapshot;

    private String changeNote;

    private LocalDateTime changedAt;
}
