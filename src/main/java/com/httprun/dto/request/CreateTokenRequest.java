package com.httprun.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建 Token 请求
 */
@Data
public class CreateTokenRequest {

    @NotBlank(message = "Token 名称不能为空")
    @Size(max = 100, message = "Token 名称长度不能超过100")
    private String name;

    /**
     * 授权的命令列表（命令名称）
     */
    private List<String> commands;

    /**
     * 是否为管理员 Token
     */
    private Boolean isAdmin = false;

    /**
     * 有效期（小时），默认24小时，0表示永不过期
     */
    private Integer expiresIn = 24;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
