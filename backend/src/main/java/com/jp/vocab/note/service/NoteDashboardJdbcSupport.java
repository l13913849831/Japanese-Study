package com.jp.vocab.note.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

final class NoteDashboardJdbcSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private NoteDashboardJdbcSupport() {
    }

    static List<String> parseTags(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(rawJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
