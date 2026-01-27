package com.httprun.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板工具类
 */
public class TemplateUtils {

    /**
     * 模板变量正则：{{.variableName}}
     */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile(
            "\\{\\{\\s*\\.([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    /**
     * 渲染模板
     */
    public static String render(String template, Map<String, String> params) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (params == null || params.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = params.getOrDefault(varName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 提取模板中的变量名
     */
    public static java.util.List<String> extractVariables(String template) {
        java.util.List<String> variables = new java.util.ArrayList<>();

        if (template == null || template.isEmpty()) {
            return variables;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (!variables.contains(varName)) {
                variables.add(varName);
            }
        }

        return variables;
    }

    /**
     * 验证模板是否有效
     */
    public static boolean isValidTemplate(String template) {
        if (template == null || template.isEmpty()) {
            return false;
        }

        // 检查是否有未闭合的模板标记
        int openCount = 0;
        int closeCount = 0;

        for (int i = 0; i < template.length() - 1; i++) {
            if (template.charAt(i) == '{' && template.charAt(i + 1) == '{') {
                openCount++;
                i++;
            } else if (template.charAt(i) == '}' && template.charAt(i + 1) == '}') {
                closeCount++;
                i++;
            }
        }

        return openCount == closeCount;
    }

    /**
     * 将参数列表转换为 Map
     */
    public static Map<String, String> paramsToMap(java.util.List<?> params) {
        Map<String, String> map = new HashMap<>();

        if (params == null) {
            return map;
        }

        for (Object param : params) {
            if (param instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) param;
                String name = String.valueOf(paramMap.get("name"));
                String value = String.valueOf(paramMap.get("value"));
                if (name != null && !"null".equals(name)) {
                    map.put(name, value != null && !"null".equals(value) ? value : "");
                }
            }
        }

        return map;
    }
}
