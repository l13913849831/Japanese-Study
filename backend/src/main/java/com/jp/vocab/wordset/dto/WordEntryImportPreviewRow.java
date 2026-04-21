package com.jp.vocab.wordset.dto;

public record WordEntryImportPreviewRow(
        long lineNumber,
        String expression,
        String reading,
        String meaning,
        String status,
        String field,
        String message
) {
}
