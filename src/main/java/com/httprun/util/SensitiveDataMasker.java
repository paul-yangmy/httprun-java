package com.httprun.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.httprun.dto.request.RunCommandRequest;
import com.httprun.entity.Command;
import com.httprun.entity.ParamDefine;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感数据脱敏工具类
 * 用于在日志记录时隐藏密码、密钥等敏感参数
 */
@Slf4j
public class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 常见敏感参数名称（关键字匹配）
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "pwd", "secret", "token", "key", "apikey",
            "api_key", "access_key", "private_key", "credential", "auth");

    /**
     * 根据命令定义脱敏参数映射
     *
     * @param command 命令定义（包含参数的 sensitive 标记）
     * @param params  参数映射
     * @return 脱敏后的参数映射
     */
    public static Map<String, Object> maskParams(Command command, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        // 获取敏感参数名称集合
        Set<String> sensitiveParamNames = getSensitiveParamNames(command);

        // 创建副本并脱敏
        Map<String, Object> masked = new HashMap<>(params);
        for (String paramName : sensitiveParamNames) {
            if (masked.containsKey(paramName)) {
                masked.put(paramName, MASK);
            }
        }

        return masked;
    }

    /**
     * 脱敏渲染后的命令字符串（替换敏感参数值）
     *
     * @param command         命令定义
     * @param renderedCommand 渲染后的完整命令
     * @param params          参数映射
     * @return 脱敏后的命令
     */
    public static String maskCommand(Command command, String renderedCommand, Map<String, Object> params) {
        if (renderedCommand == null || params == null) {
            return renderedCommand;
        }

        Set<String> sensitiveParamNames = getSensitiveParamNames(command);
        String masked = renderedCommand;

        // 替换敏感参数的值
        for (String paramName : sensitiveParamNames) {
            Object value = params.get(paramName);
            if (value != null) {
                String valueStr = String.valueOf(value);
                if (!valueStr.isEmpty()) {
                    // 使用正则避免部分匹配问题
                    masked = masked.replace(valueStr, MASK);
                }
            }
        }

        return masked;
    }

    /**
     * 脱敏 JSON 请求体（用于访问日志）
     *
     * @param command     命令定义
     * @param requestJson JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String maskRequestJson(Command command, String requestJson) {
        if (requestJson == null || requestJson.isEmpty()) {
            return requestJson;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(requestJson);
            if (!rootNode.isObject()) {
                return requestJson;
            }

            ObjectNode objectNode = (ObjectNode) rootNode;
            Set<String> sensitiveParamNames = getSensitiveParamNames(command);

            // 处理 params 数组
            if (objectNode.has("params") && objectNode.get("params").isArray()) {
                JsonNode paramsArray = objectNode.get("params");
                for (JsonNode paramNode : paramsArray) {
                    if (paramNode.has("name") && paramNode.has("value")) {
                        String paramName = paramNode.get("name").asText();
                        if (isSensitiveParam(paramName, sensitiveParamNames)) {
                            ((ObjectNode) paramNode).put("value", MASK);
                        }
                    }
                }
            }

            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            log.warn("Failed to mask request JSON, returning original", e);
            return requestJson;
        }
    }

    /**
     * 脱敏 RunCommandRequest 对象（用于访问日志）
     *
     * @param command 命令定义
     * @param request 请求对象
     * @return 脱敏后的 JSON 字符串
     */
    public static String maskRequest(Command command, RunCommandRequest request) {
        if (request == null) {
            return null;
        }

        try {
            // 创建副本避免修改原对象
            RunCommandRequest copy = cloneRequest(request);
            Set<String> sensitiveParamNames = getSensitiveParamNames(command);

            if (copy.getParams() != null) {
                for (var param : copy.getParams()) {
                    if (isSensitiveParam(param.getName(), sensitiveParamNames)) {
                        param.setValue(MASK);
                    }
                }
            }

            return objectMapper.writeValueAsString(copy);
        } catch (Exception e) {
            log.warn("Failed to mask request, returning original", e);
            try {
                return objectMapper.writeValueAsString(request);
            } catch (Exception ex) {
                return request.toString();
            }
        }
    }

    /**
     * 获取命令中定义的敏感参数名称集合
     */
    private static Set<String> getSensitiveParamNames(Command command) {
        if (command == null || command.getCommandConfig() == null
                || command.getCommandConfig().getParams() == null) {
            return Set.of();
        }

        return command.getCommandConfig().getParams().stream()
                .filter(ParamDefine::isSensitive)
                .map(ParamDefine::getName)
                .collect(Collectors.toSet());
    }

    /**
     * 判断参数是否为敏感参数
     * 1. 显式标记为 sensitive
     * 2. 参数名包含敏感关键字
     */
    private static boolean isSensitiveParam(String paramName, Set<String> sensitiveParamNames) {
        if (paramName == null) {
            return false;
        }

        // 1. 显式标记
        if (sensitiveParamNames.contains(paramName)) {
            return true;
        }

        // 2. 关键字匹配
        String lowerParamName = paramName.toLowerCase();
        return SENSITIVE_KEYWORDS.stream().anyMatch(lowerParamName::contains);
    }

    /**
     * 克隆 RunCommandRequest（浅拷贝参数列表）
     */
    private static RunCommandRequest cloneRequest(RunCommandRequest request) {
        RunCommandRequest copy = new RunCommandRequest();
        copy.setName(request.getName());
        copy.setAsync(request.getAsync());
        copy.setTimeout(request.getTimeout());
        copy.setEnv(request.getEnv());

        if (request.getParams() != null) {
            copy.setParams(request.getParams().stream()
                    .map(param -> {
                        RunCommandRequest.ParamInput newParam = new RunCommandRequest.ParamInput();
                        newParam.setName(param.getName());
                        newParam.setValue(param.getValue());
                        return newParam;
                    })
                    .collect(Collectors.toList()));
        }

        return copy;
    }
}
