package com.httprun.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撤销 Token 响应
 * 当撤销的是管理员 Token 时，会自动生成新的管理员 Token 并返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "撤销 Token 响应")
public class RevokeTokenResponse {

    @Schema(description = "操作是否成功", example = "true")
    private boolean success;

    @Schema(description = "提示消息", example = "Token 已撤销")
    private String message;

    @Schema(description = "是否生成了新的管理员 Token", example = "true")
    private boolean newAdminTokenGenerated;

    @Schema(description = "新生成的管理员 Token ID（仅当撤销管理员 Token 时返回）", example = "2")
    private Long newTokenId;

    @Schema(description = "新生成的管理员 Token 名称", example = "admin")
    private String newTokenName;

    @Schema(description = "新生成的 JWT Token（仅当撤销管理员 Token 时返回，请妥善保存！）")
    private String newJwtToken;

    @Schema(description = "警告信息", example = "请立即保存新的管理员 Token，此信息仅显示一次！")
    private String warning;

    /**
     * 创建普通撤销响应（非管理员 Token）
     */
    public static RevokeTokenResponse normalRevoke() {
        return RevokeTokenResponse.builder()
                .success(true)
                .message("Token 已撤销")
                .newAdminTokenGenerated(false)
                .build();
    }

    /**
     * 创建管理员 Token 撤销响应（包含新生成的 Token）
     */
    public static RevokeTokenResponse adminRevoke(Long newTokenId, String newTokenName, String newJwtToken) {
        return RevokeTokenResponse.builder()
                .success(true)
                .message("管理员 Token 已撤销，已自动生成新的管理员 Token")
                .newAdminTokenGenerated(true)
                .newTokenId(newTokenId)
                .newTokenName(newTokenName)
                .newJwtToken(newJwtToken)
                .warning("请立即保存新的管理员 Token，此信息仅显示一次！关闭后将无法再次查看。")
                .build();
    }
}
