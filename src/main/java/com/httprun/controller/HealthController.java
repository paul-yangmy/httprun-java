package com.httprun.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@Tag(name = "Health", description = "健康检查接口")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()));
    }

    @GetMapping("/")
    @Operation(summary = "首页")
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "name", "HttpRun",
                "version", "1.0.0",
                "description", "HTTP API Shell Command Gateway"));
    }
}
