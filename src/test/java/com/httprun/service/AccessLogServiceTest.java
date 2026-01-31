package com.httprun.service;

import com.httprun.dto.AuditContext;
import com.httprun.entity.AccessLog;
import com.httprun.repository.AccessLogRepository;
import com.httprun.service.impl.AccessLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AccessLogService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AccessLogServiceTest {

    @Mock
    private AccessLogRepository accessLogRepository;

    @InjectMocks
    private AccessLogServiceImpl accessLogService;

    private AuditContext testContext;
    private AccessLog testLog;

    @BeforeEach
    void setUp() {
        testContext = AuditContext.builder()
                .tokenId("test-token")
                .path("/api/commands/run")
                .ip("192.168.1.100")
                .method("POST")
                .userAgent("Mozilla/5.0")
                .source("WEB")
                .requestId("req-12345")
                .commandName("deploy-app")
                .statusCode(200)
                .duration(1500L)
                .build();

        testLog = new AccessLog();
        testLog.setId(1L);
        testLog.setTokenId("test-token");
        testLog.setPath("/api/commands/run");
        testLog.setIp("192.168.1.100");
        testLog.setStatusCode(200);
    }

    @Test
    void logAccess_shouldSaveAccessLog() {
        // When
        accessLogService.logAccess(testContext);

        // Then
        verify(accessLogRepository, timeout(1000)).save(any(AccessLog.class));
    }

    @Test
    void logAccess_legacyMethod_shouldConvertToAuditContext() {
        // When
        accessLogService.logAccess("token", "/api/test", "127.0.0.1", "GET",
                "{}", "{\"status\":\"ok\"}", 200, 100L);

        // Then
        verify(accessLogRepository, timeout(1000)).save(any(AccessLog.class));
    }

    @Test
    void listLogs_shouldReturnPagedLogs() {
        // Given
        Page<AccessLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(accessLogRepository.findAll(any(PageRequest.class))).thenReturn(expectedPage);

        // When
        Page<AccessLog> result = accessLogService.listLogs(1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTokenId()).isEqualTo("test-token");
        verify(accessLogRepository).findAll(any(PageRequest.class));
    }

    @Test
    void listLogsByToken_shouldReturnFilteredLogs() {
        // Given
        Page<AccessLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(accessLogRepository.findByTokenId(eq("test-token"), any(PageRequest.class)))
                .thenReturn(expectedPage);

        // When
        Page<AccessLog> result = accessLogService.listLogsByToken("test-token", 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(accessLogRepository).findByTokenId(eq("test-token"), any(PageRequest.class));
    }

    @Test
    void listLogsByIp_shouldReturnFilteredLogs() {
        // Given
        Page<AccessLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(accessLogRepository.findByIp(eq("192.168.1.100"), any(PageRequest.class)))
                .thenReturn(expectedPage);

        // When
        Page<AccessLog> result = accessLogService.listLogsByIp("192.168.1.100", 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(accessLogRepository).findByIp(eq("192.168.1.100"), any(PageRequest.class));
    }

    @Test
    void listLogsBySource_shouldReturnFilteredLogs() {
        // Given
        Page<AccessLog> expectedPage = new PageImpl<>(List.of(testLog));
        when(accessLogRepository.findBySource(eq("WEB"), any(PageRequest.class)))
                .thenReturn(expectedPage);

        // When
        Page<AccessLog> result = accessLogService.listLogsBySource("WEB", 1, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(accessLogRepository).findBySource(eq("WEB"), any(PageRequest.class));
    }

    @Test
    void cleanOldLogs_shouldDeleteOldRecords() {
        // Given
        when(accessLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(50);

        // When
        int deleted = accessLogService.cleanOldLogs(30);

        // Then
        assertThat(deleted).isEqualTo(50);
        verify(accessLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
