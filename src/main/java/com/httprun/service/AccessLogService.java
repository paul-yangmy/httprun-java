package com.httprun.service;

import com.httprun.dto.AuditContext;
import com.httprun.entity.AccessLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

/**
 * 访问日志服务接口
 */
public interface AccessLogService {

        /**
         * 记录访问日志（增强版）
         *
         * @param context 审计上下文
         */
        void logAccess(AuditContext context);

        /**
         * 记录访问日志（兼容旧版）
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
         * 根据 IP 查询日志
         */
        Page<AccessLog> listLogsByIp(String ip, int page, int pageSize);

        /**
         * 根据来源类型查询日志
         */
        Page<AccessLog> listLogsBySource(String source, int page, int pageSize);

        /**
         * 清理历史日志
         *
         * @param retentionDays 保留天数
         * @return 删除的日志数量
         */
        int cleanOldLogs(int retentionDays);

        /**
         * 高级搜索日志（支持多条件筛选）
         *
         * @param tokenId     Token ID（可选，为空则查询所有）
         * @param commandName 命令名称（可选）
         * @param status      状态筛选：success/error（可选）
         * @param startTime   开始时间（可选）
         * @param endTime     结束时间（可选）
         * @param keyword     关键词搜索（路径或命令名称）
         * @param page        页码
         * @param pageSize    每页数量
         * @return 分页日志
         */
        Page<AccessLog> searchLogs(String tokenId, String commandName, String status,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String keyword, int page, int pageSize);

        /**
         * 高级搜索日志（支持多条件筛选，可选择只查询命令执行记录）
         *
         * @param tokenId     Token ID（可选，为空则查询所有）
         * @param commandName 命令名称（可选）
         * @param status      状态筛选：success/error（可选）
         * @param startTime   开始时间（可选）
         * @param endTime     结束时间（可选）
         * @param keyword     关键词搜索（路径或命令名称）
         * @param commandOnly 是否只查询命令执行记录
         * @param page        页码
         * @param pageSize    每页数量
         * @return 分页日志
         */
        Page<AccessLog> searchLogs(String tokenId, String commandName, String status,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String keyword, boolean commandOnly, int page, int pageSize);

        /**
         * 删除指定日志
         *
         * @param id 日志 ID
         */
        void deleteLog(Long id);

        /**
         * 批量删除日志
         *
         * @param ids 日志 ID 列表
         * @return 删除数量
         */
        int deleteLogs(java.util.List<Long> ids);

        /**
         * 根据 Token 删除日志（用户清空自己的记录）
         *
         * @param tokenId Token ID
         * @return 删除数量
         */
        int deleteLogsByToken(String tokenId);

        /**
         * 根据 ID 获取日志
         */
        AccessLog getLogById(Long id);
}
