package com.httprun.repository;

import com.httprun.entity.Token;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Token 数据访问层
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    /**
     * 根据 JWT Token 查找
     */
    Optional<Token> findByJwtToken(String jwtToken);

    /**
     * 根据名称查找
     */
    List<Token> findByName(String name);

    /**
     * 检查 JWT Token 是否存在
     */
    boolean existsByJwtToken(String jwtToken);

    /**
     * 查找未撤销的有效 Token（使用 Unix 时间戳）
     */
    @Query("SELECT t FROM Token t WHERE t.jwtToken = :jwtToken AND t.revoked = false AND t.expiresAt > :now")
    Optional<Token> findValidToken(@Param("jwtToken") String jwtToken, @Param("now") Long now);

    /**
     * 分页查询 Token
     */
    Page<Token> findByRevoked(boolean revoked, Pageable pageable);

    /**
     * 查找过期的 Token
     */
    List<Token> findByExpiresAtBeforeAndRevokedFalse(Long timestamp);

    /**
     * 撤销 Token
     */
    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.id = :id")
    int revokeToken(@Param("id") Long id);

    /**
     * 批量撤销过期 Token
     */
    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.expiresAt < :now AND t.revoked = false")
    int revokeExpiredTokens(@Param("now") Long now);

    /**
     * 根据 ID 列表删除
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    /**
     * 根据名称列表删除
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.name IN :names")
    int deleteByNameIn(@Param("names") List<String> names);

    /**
     * 检查是否存在未撤销的 Token
     */
    boolean existsByNameAndRevokedFalse(String name);

    /**
     * 检查 JWT Token 是否有效（未撤销且未过期）
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Token t " +
            "WHERE t.jwtToken = :jwtToken AND t.revoked = false AND t.expiresAt > :now")
    boolean existsValidToken(@Param("jwtToken") String jwtToken, @Param("now") Long now);
}
