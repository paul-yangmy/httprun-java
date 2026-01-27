package com.httprun.service.impl;

import com.httprun.entity.AccessLog;
import com.httprun.repository.AccessLogRepository;
import com.httprun.service.AccessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 访问日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogRepository accessLogRepository;

    @Override
    @Async
    @Transactional
    public void logAccess(String tokenId, String path, String ip, String method,
            String request, String response, Integer statusCode, Long duration) {
        AccessLog accessLog = new AccessLog();
        accessLog.setTokenId(tokenId);
        accessLog.setPath(path);
        accessLog.setIp(ip);
        accessLog.setMethod(method);
        accessLog.setRequest(request);
        // 限制响应内容长度
        accessLog.setResponse(response != null && response.length() > 65000
                ? response.substring(0, 65000)
                : response);
        accessLog.setStatusCode(statusCode);
        accessLog.setDuration(duration);

        accessLogRepository.save(accessLog);

        log.debug("Logged access: path={}, statusCode={}, duration={}ms", path, statusCode, duration);
    }

    @Override
    public Page<AccessLog> listLogs(int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findAll(pageRequest);
    }

    @Override
    public Page<AccessLog> listLogsByToken(String tokenId, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findByTokenId(tokenId, pageRequest);
    }

    @Override
    public Page<AccessLog> listLogsByPath(String path, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findByPath(path, pageRequest);
    }

    @Override
    public Page<AccessLog> listLogsByTimeRange(LocalDateTime start, LocalDateTime end,
            int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findByCreatedAtBetween(start, end, pageRequest);
    }

    @Override
    @Transactional
    public int cleanOldLogs(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        int deleted = accessLogRepository.deleteByCreatedAtBefore(cutoffTime);
        log.info("Cleaned {} old access logs older than {} days", deleted, retentionDays);
        return deleted;
    }

    /**
     * 定时清理任务 - 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledCleanup() {
        int deleted = cleanOldLogs(30); // 保留 30 天
        log.info("Scheduled cleanup: removed {} old access logs", deleted);
    }
}
