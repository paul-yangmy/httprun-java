package com.httprun.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建 Token 请求
 */
@Data
@Schema(description = "创建 Token 请求对象")
public class CreateTokenRequest {

    @Schema(description = "Token 名称", example = "my-api-token", required = true)
    @NotBlank(message = "Token 名称不能为空")
    @Size(max = 100, message = "Token 名称长度不能超过100")
    private String name;

    /**
     * 授权的命令列表（命令名称）
     */
    @Schema(description = "授权的命令列表（空表示全部命令）", example = "[\"deploy-app\", \"backup-db\"]")
    private List<String> commands;

    /**
     * 是否为管理员 Token
     */
    @Schema(description = "是否为管理员 Token（管理员可执行所有命令）", example = "false", defaultValue = "false")
    private Boolean isAdmin = false;

    /**
     * 有效期（小时），默认24小时，0表示永不过期
     */
    @Schema(description = "有效期（小时），0 表示永不过期", example = "24", defaultValue = "24")
    private Integer expiresIn = 24;

    /**
     * 备注
     */
    @Schema(description = "备注信息", example = "用于 CI/CD 集成")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
