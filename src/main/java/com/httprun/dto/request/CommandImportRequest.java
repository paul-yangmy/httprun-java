package com.httprun.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 命令导入请求
 */
@Data
@Schema(description = "命令导入请求")
public class CommandImportRequest {

    @Schema(description = "要导入的命令列表", required = true)
    @NotNull(message = "命令列表不能为空")
    private List<CreateCommandRequest> commands;

    /**
     * 导入模式：skip（跳过同名命令）/ overwrite（覆盖同名命令），默认 skip
     */
    @Schema(description = "导入模式：skip（跳过同名命令）/ overwrite（覆盖同名命令）", example = "skip", defaultValue = "skip")
    private String mode = "skip";
}
