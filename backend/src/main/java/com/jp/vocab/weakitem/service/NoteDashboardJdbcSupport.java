package com.jp.vocab.weakitem.service;

import java.util.List;

final class NoteDashboardJdbcSupport {

    private NoteDashboardJdbcSupport() {
    }

    static List<String> parseTags(String rawJson) {
        if (rawJson == null || rawJson.isBlank() || rawJson.equals("[]")) {
            return List.of();
        }

        return List.of(rawJson.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
