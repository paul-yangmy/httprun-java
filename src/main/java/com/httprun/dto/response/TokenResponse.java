package com.httprun.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private Long id;

    private String name;

    /**
     * JWT Token（仅创建时返回）
     */
    private String token;

    /**
     * 授权的命令列表
     */
    private List<String> commands;

    private Boolean isAdmin;

    private Boolean revoked;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private String lastUsedIp;

    private String remark;

    private LocalDateTime createdAt;
}
