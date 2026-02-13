package com.httprun.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.EnvVar;
import com.httprun.entity.RemoteConfig;
import com.httprun.enums.CommandStatus;
import com.httprun.exception.BusinessException;
import com.httprun.executor.CommandTemplate;
import com.httprun.repository.CommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 命令流 WebSocket 处理器
 * 实时推送命令执行输出到客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandStreamHandler extends TextWebSocketHandler {

    private final CommandRepository commandRepository;
    private final CommandTemplate commandTemplate;
    private final ObjectMapper objectMapper;
    private final com.httprun.executor.SshCommandExecutor sshCommandExecutor;

    // 存储活跃的执行进程，支持取消
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    // SSH 流式执行的取消回调
    private final Map<String, Runnable> activeCancelCallbacks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: {}, user: {}",
                session.getId(), session.getAttributes().get("name"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            StreamRequest request = objectMapper.readValue(payload, StreamRequest.class);

            switch (request.getType()) {
                case "run" -> handleRunCommand(session, request);
                case "cancel" -> handleCancel(session);
                default -> sendError(session, "Unknown message type: " + request.getType());
            }
        } catch (Exception e) {
            log.error("Handle message error", e);
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket disconnected: {}, status: {}", session.getId(), status);
        // 取消可能正在运行的进程
        cancelProcess(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", session.getId(), exception);
        cancelProcess(session.getId());
    }

    /**
     * 处理命令执行请求
     */
    private void handleRunCommand(WebSocketSession session, StreamRequest request) {
        String sessionId = session.getId();
        String subject = (String) session.getAttributes().get("subject");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttributes().get("isAdmin"));

        try {
            // 1. 查询命令
            Command command = commandRepository.findByName(request.getName())
                    .orElseThrow(() -> new BusinessException("Command not found: " + request.getName()));

            // 2. 检查命令状态
            if (command.getStatus() != CommandStatus.ACTIVE) {
                sendError(session, "Command is inactive");
                return;
            }

            // 3. 检查权限
            if (!isAdmin && subject != null) {
                List<String> allowedCommands = Arrays.asList(subject.split(","));
                if (!allowedCommands.contains(command.getName())) {
                    sendError(session, "Permission denied");
                    return;
                }
            }

            // 4. 构建 RunCommandRequest
            RunCommandRequest runRequest = new RunCommandRequest();
            runRequest.setName(request.getName());
            runRequest.setParams(request.getParams());
            runRequest.setEnv(request.getEnv());
            // 支持通过 WebSocket 传入 remoteConfig（用于 SSH 流式输出）
            runRequest.setRemoteConfig(request.getRemoteConfig());

            // 5. 验证参数
            commandTemplate.validateParams(command, runRequest);

            // 6. 渲染命令模板
            String[] rendered = commandTemplate.renderWithMasking(command, runRequest);
            String actualCommand = rendered[0];
            String maskedCommand = rendered[1];
            log.info("Streaming command: {} (masked)", maskedCommand);

            // 7. 发送开始信号
            sendMessage(session, new StreamMessage("start", null, null, null));

            // 8. 执行命令并流式输出
            int timeout = request.getTimeout() != null ? request.getTimeout() : command.getTimeoutSeconds();
            // 如果请求中包含 remoteConfig，则使用 SSH 流式执行
            RemoteConfig reqRemote = request.getRemoteConfig();
            if (reqRemote != null && reqRemote.getHost() != null && !reqRemote.getHost().isBlank()) {
                // SSH 流式执行在独立线程中进行，注册取消回调
                Thread t = new Thread(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        int exit = sshCommandExecutor.executeStreaming(reqRemote, actualCommand, timeout,
                                (type, line) -> {
                                    if ("stdout".equals(type)) {
                                        sendMessage(session, new StreamMessage("stdout", line, null, null));
                                    } else {
                                        sendMessage(session, new StreamMessage("stderr", null, line, null));
                                    }
                                }, (cancelFn) -> activeCancelCallbacks.put(sessionId, cancelFn));

                        long duration = System.currentTimeMillis() - startTime;
                        sendComplete(session, exit, duration);
                    } catch (Exception e) {
                        log.error("SSH stream execution error", e);
                        sendError(session, e.getMessage());
                        sendComplete(session, -1, System.currentTimeMillis() - startTime);
                    } finally {
                        activeCancelCallbacks.remove(sessionId);
                    }
                }, "ssh-stream-" + sessionId);
                t.start();
            } else {
                executeWithStreaming(session, actualCommand, timeout);
            }

        } catch (BusinessException e) {
            sendError(session, e.getMessage());
        } catch (Exception e) {
            log.error("Command execution error", e);
            sendError(session, "Internal error: " + e.getMessage());
        }
    }

    /**
     * 流式执行命令
     */
    private void executeWithStreaming(WebSocketSession session, String command, int timeoutSeconds) {
        String sessionId = session.getId();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 解析命令
            List<String> cmdArgs = parseCommand(command);
            log.debug("Executing stream command: {}", cmdArgs);

            // 2. 构建进程
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs);
            processBuilder.redirectErrorStream(false);

            // 3. 启动进程
            Process process = processBuilder.start();
            activeProcesses.put(sessionId, process);

            // 4. 创建输出读取线程
            Charset charset = System.getProperty("os.name").toLowerCase().contains("windows")
                    ? Charset.forName("GBK")
                    : Charset.forName("UTF-8");

            // stdout 读取线程
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sendMessage(session, new StreamMessage("stdout", line, null, null));
                    }
                } catch (IOException e) {
                    if (!e.getMessage().contains("closed")) {
                        log.debug("Stdout read error: {}", e.getMessage());
                    }
                }
            }, "stdout-reader-" + sessionId);

            // stderr 读取线程
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sendMessage(session, new StreamMessage("stderr", null, line, null));
                    }
                } catch (IOException e) {
                    if (!e.getMessage().contains("closed")) {
                        log.debug("Stderr read error: {}", e.getMessage());
                    }
                }
            }, "stderr-reader-" + sessionId);

            stdoutThread.start();
            stderrThread.start();

            // 5. 等待进程完成（带超时）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                sendError(session, "Command timeout after " + timeoutSeconds + " seconds");
                sendComplete(session, -1, System.currentTimeMillis() - startTime);
                return;
            }

            // 6. 等待读取线程结束
            stdoutThread.join(5000);
            stderrThread.join(5000);

            // 7. 发送完成信号
            int exitCode = process.exitValue();
            long duration = System.currentTimeMillis() - startTime;
            sendComplete(session, exitCode, duration);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendError(session, "Execution interrupted");
            sendComplete(session, -1, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Stream execution error", e);
            sendError(session, e.getMessage());
            sendComplete(session, -1, System.currentTimeMillis() - startTime);
        } finally {
            activeProcesses.remove(sessionId);
        }
    }

    /**
     * 处理取消请求
     */
    private void handleCancel(WebSocketSession session) {
        String sessionId = session.getId();
        boolean cancelled = cancelProcess(sessionId);
        Runnable cancelCb = activeCancelCallbacks.remove(sessionId);
        if (cancelCb != null) {
            try {
                cancelCb.run();
                cancelled = true;
            } catch (Exception ignored) {
            }
        }
        if (cancelled) {
            sendMessage(session, new StreamMessage("cancelled", null, null, null));
            log.info("Command cancelled for session: {}", sessionId);
        } else {
            sendMessage(session, new StreamMessage("cancelled", null, null, null));
        }
    }

    /**
     * 取消进程
     */
    private boolean cancelProcess(String sessionId) {
        Process process = activeProcesses.remove(sessionId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            return true;
        }
        return false;
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, StreamMessage msg) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(msg);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Send message error", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new StreamMessage("error", null, error, null));
    }

    /**
     * 发送完成信号
     */
    private void sendComplete(WebSocketSession session, int exitCode, long duration) {
        StreamMessage msg = new StreamMessage("complete", null, null, null);
        msg.setExitCode(exitCode);
        msg.setDuration(duration);
        sendMessage(session, msg);
    }

    /**
     * 解析命令字符串为参数列表
     */
    private List<String> parseCommand(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        args.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    /**
     * 流式请求 DTO
     */
    @lombok.Data
    public static class StreamRequest {
        private String type; // run, cancel
        private String name; // 命令名称
        private List<RunCommandRequest.ParamInput> params;
        private List<EnvVar> env;
        private Integer timeout;
        private com.httprun.entity.RemoteConfig remoteConfig;
    }

    /**
     * 流式响应 DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class StreamMessage {
        private String type; // start, stdout, stderr, error, complete, cancelled
        private String stdout;
        private String stderr;
        private String error;
        private Integer exitCode;
        private Long duration;

        public StreamMessage(String type, String stdout, String stderr, String error) {
            this.type = type;
            this.stdout = stdout;
            this.stderr = stderr;
            this.error = error;
        }
    }
}
