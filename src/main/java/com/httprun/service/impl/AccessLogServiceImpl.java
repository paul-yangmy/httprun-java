package com.httprun.service.impl;

import com.httprun.dto.AuditContext;
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
    public void logAccess(AuditContext context) {
        AccessLog accessLog = new AccessLog();
        accessLog.setTokenId(context.getTokenId());
        accessLog.setPath(context.getPath());
        accessLog.setIp(context.getIp());
        accessLog.setMethod(context.getMethod());

        // 审计增强字段
        accessLog.setUserAgent(truncate(context.getUserAgent(), 500));
        accessLog.setReferer(truncate(context.getReferer(), 500));
        accessLog.setSource(context.getSource());
        accessLog.setForwardedFor(truncate(context.getForwardedFor(), 200));
        accessLog.setRequestId(context.getRequestId());
        accessLog.setCommandName(context.getCommandName());

        accessLog.setRequest(context.getRequest());
        // 限制响应内容长度
        accessLog.setResponse(truncate(context.getResponse(), 65000));
        accessLog.setStatusCode(context.getStatusCode());
        accessLog.setDuration(context.getDuration());

        accessLogRepository.save(accessLog);

        log.debug("Logged access: path={}, ip={}, source={}, statusCode={}, duration={}ms",
                context.getPath(), context.getIp(), context.getSource(),
                context.getStatusCode(), context.getDuration());
    }

    @Override
    @Async
    @Transactional
    public void logAccess(String tokenId, String path, String ip, String method,
            String request, String response, Integer statusCode, Long duration) {
        // 兼容旧版调用，转换为 AuditContext
        AuditContext context = AuditContext.builder()
                .tokenId(tokenId)
                .path(path)
                .ip(ip)
                .method(method)
                .request(request)
                .response(response)
                .statusCode(statusCode)
                .duration(duration)
                .source("API") // 默认来源
                .build();
        logAccess(context);
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
    public Page<AccessLog> listLogsByIp(String ip, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findByIp(ip, pageRequest);
    }

    @Override
    public Page<AccessLog> listLogsBySource(String source, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return accessLogRepository.findBySource(source, pageRequest);
    }

    @Override
    @Transactional
    public int cleanOldLogs(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        int deleted = accessLogRepository.deleteByCreatedAtBefore(cutoffTime);
        log.info("Cleaned {} old access logs older than {} days", deleted, retentionDays);
        return deleted;
    }

    @Override
    public Page<AccessLog> searchLogs(String tokenId, String commandName, String status,
            LocalDateTime startTime, LocalDateTime endTime,
            String keyword, int page, int pageSize) {
        // 向后兼容，调用新方法，默认不过滤
        return searchLogs(tokenId, commandName, status, startTime, endTime, keyword, false, page, pageSize);
    }

    @Override
    public Page<AccessLog> searchLogs(String tokenId, String commandName, String status,
            LocalDateTime startTime, LocalDateTime endTime,
            String keyword, boolean commandOnly, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, page - 1),
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // 使用 Specification 动态构建查询条件
        return accessLogRepository.findAll((root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            // 只查询命令执行记录（commandName 不为空）
            if (commandOnly) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("commandName")),
                        cb.notEqual(root.get("commandName"), "")));
            }

            // Token 筛选
            if (tokenId != null && !tokenId.isEmpty()) {
                predicates.add(cb.equal(root.get("tokenId"), tokenId));
            }

            // 命令名称筛选
            if (commandName != null && !commandName.isEmpty()) {
                predicates.add(cb.equal(root.get("commandName"), commandName));
            }

            // 状态筛选
            if (status != null && !status.isEmpty()) {
                if ("success".equalsIgnoreCase(status)) {
                    predicates.add(cb.between(root.get("statusCode"), 200, 299));
                } else if ("error".equalsIgnoreCase(status)) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("statusCode"), 400));
                }
            }

            // 时间范围筛选
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            // 关键词搜索（路径或命令名称）
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("path")), pattern),
                        cb.like(cb.lower(root.get("commandName")), pattern)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageRequest);
    }

    @Override
    @Transactional
    public void deleteLog(Long id) {
        accessLogRepository.deleteById(id);
        log.info("Deleted access log: id={}", id);
    }

    @Override
    @Transactional
    public int deleteLogs(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        accessLogRepository.deleteAllById(ids);
        log.info("Deleted {} access logs", ids.size());
        return ids.size();
    }

    @Override
    @Transactional
    public int deleteLogsByToken(String tokenId) {
        int deleted = accessLogRepository.deleteByTokenId(tokenId);
        log.info("Deleted {} access logs for token: {}", deleted, tokenId);
        return deleted;
    }

    @Override
    public AccessLog getLogById(Long id) {
        return accessLogRepository.findById(id).orElse(null);
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

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
