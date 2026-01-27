package com.httprun.service;

import com.httprun.entity.Token;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Token 服务接口
 */
public interface TokenService {

    /**
     * 创建 Token
     *
     * @param name      Token 名称
     * @param subject   授权的命令列表（逗号分隔）
     * @param isAdmin   是否管理员
     * @param expiresIn 过期时间（小时），0 表示永不过期（默认 1 年）
     * @return 创建的 Token
     */
    Token createToken(String name, String subject, boolean isAdmin, int expiresIn);

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
     */
    void revokeToken(Long id);

    /**
     * 批量删除 Token
     */
    void deleteTokens(List<Long> ids);

    /**
     * 验证 Token
     */
    boolean validateToken(String jwtToken);

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
