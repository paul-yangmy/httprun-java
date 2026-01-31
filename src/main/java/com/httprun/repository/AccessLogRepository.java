package com.httprun.repository;

import com.httprun.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 访问日志数据访问层
 */
@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    /**
     * 根据 Token ID 查找日志
     */
    Page<AccessLog> findByTokenId(String tokenId, Pageable pageable);

    /**
     * 根据路径查找日志
     */
    Page<AccessLog> findByPath(String path, Pageable pageable);

    /**
     * 根据时间范围查找日志
     */
    Page<AccessLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 根据状态码查找日志
     */
    List<AccessLog> findByStatusCode(Integer statusCode);

    /**
     * 删除指定时间之前的日志
     */
    @Modifying
    @Query("DELETE FROM AccessLog a WHERE a.createdAt < :cutoffTime")
    int deleteByCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 统计指定时间范围内的请求数
     */
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.createdAt BETWEEN :start AND :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 根据 IP 地址查找日志
     */
    Page<AccessLog> findByIp(String ip, Pageable pageable);

    /**
     * 根据来源查找日志
     */
    Page<AccessLog> findBySource(String source, Pageable pageable);

    /**
     * 根据请求 ID 查找日志
     */
    AccessLog findByRequestId(String requestId);

    /**
     * 根据命令名称查找日志
     */
    Page<AccessLog> findByCommandName(String commandName, Pageable pageable);
}
