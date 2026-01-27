package com.httprun.service;

import com.httprun.entity.AccessLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

/**
 * 访问日志服务接口
 */
public interface AccessLogService {

    /**
     * 记录访问日志
     *
     * @param tokenId    Token ID
     * @param path       请求路径
     * @param ip         客户端 IP
     * @param method     请求方法
     * @param request    请求内容
     * @param response   响应内容
     * @param statusCode 状态码
     * @param duration   耗时（毫秒）
     */
    void logAccess(String tokenId, String path, String ip, String method,
            String request, String response, Integer statusCode, Long duration);

    /**
     * 获取日志列表（分页）
     */
    Page<AccessLog> listLogs(int page, int pageSize);

    /**
     * 根据 Token 查询日志
     */
    Page<AccessLog> listLogsByToken(String tokenId, int page, int pageSize);

    /**
     * 根据路径查询日志
     */
    Page<AccessLog> listLogsByPath(String path, int page, int pageSize);

    /**
     * 根据时间范围查询日志
     */
    Page<AccessLog> listLogsByTimeRange(LocalDateTime start, LocalDateTime end, int page, int pageSize);

    /**
     * 清理历史日志
     *
     * @param retentionDays 保留天数
     * @return 删除的日志数量
     */
    int cleanOldLogs(int retentionDays);
}
