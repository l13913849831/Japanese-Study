package com.jp.vocab.wordset.service;

import java.util.Map;

record AnalyzedImportRow(
        long lineNumber,
        Map<String, String> values,
        String status,
        String field,
        String message
) {
    boolean isReady() {
        return "READY".equals(status);
    }

    boolean isDuplicate() {
        return "DUPLICATE".equals(status);
    }

    boolean isError() {
        return "ERROR".equals(status);
    }
}
