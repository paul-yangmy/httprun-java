package com.httprun.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA AttributeConverter：将 RemoteConfig 与数据库 JSON 列互转，确保 SSH 配置正确持久化与读取。
 */
@Slf4j
@Converter(autoApply = false)
public class RemoteConfigConverter implements AttributeConverter<RemoteConfig, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(RemoteConfig attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RemoteConfig to JSON", e);
            throw new IllegalArgumentException("RemoteConfig serialization failed", e);
        }
    }

    @Override
    public RemoteConfig convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, RemoteConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize RemoteConfig from JSON: {}", dbData, e);
            throw new IllegalArgumentException("RemoteConfig deserialization failed", e);
        }
    }
}
