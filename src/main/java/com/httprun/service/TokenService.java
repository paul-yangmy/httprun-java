package com.httprun.service;

import com.httprun.dto.request.CreateTokenRequest;
import com.httprun.dto.response.RevokeTokenResponse;
import com.httprun.entity.Token;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Token 服务接口
 */
public interface TokenService {

    /**
     * 创建 Token（简化版本，兼容旧接口）
     *
     * @param name      Token 名称
     * @param subject   授权的命令列表（逗号分隔）
     * @param isAdmin   是否管理员
     * @param expiresIn 过期时间（小时），0 表示永不过期（默认 1 年）
     * @return 创建的 Token
     */
    Token createToken(String name, String subject, boolean isAdmin, int expiresIn);

    /**
     * 创建 Token（完整版本，支持时间范围限制）
     *
     * @param request 创建 Token 请求
     * @return 创建的 Token
     */
    Token createToken(CreateTokenRequest request);

    /**
     * 获取 Token 详情
     */
    Token getToken(Long id);

    /**
     * 获取所有 Token 列表
     */
    List<Token> listAllTokens();

    /**
     * 分页获取 Token 列表
     */
    Page<Token> listTokens(int page, int pageSize);

    /**
     * 撤销 Token
     * 如果撤销的是管理员 Token，会自动生成新的管理员 Token
     * 
     * @param id Token ID
     * @return 撤销响应（包含新管理员 Token 信息，如果适用）
     */
    RevokeTokenResponse revokeToken(Long id);

    /**
     * 批量删除 Token
     */
    void deleteTokens(List<Long> ids);

    /**
     * 验证 Token（仅验证签名和过期时间）
     */
    boolean validateToken(String jwtToken);

    /**
     * 验证 Token 时间范围权限
     * 
     * @param jwtToken JWT Token 字符串
     * @return 是否在允许的时间范围内
     */
    boolean validateTokenTimeRange(String jwtToken);

    /**
     * 根据 JWT Token 获取 Token 实体
     */
    Token getTokenByJwt(String jwtToken);

    /**
     * 清理过期 Token
     *
     * @return 清理的数量
     */
    int cleanExpiredTokens();
}
