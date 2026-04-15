package com.jp.vocab.wordset.dto;

public record WordEntryImportError(
        long lineNumber,
        String field,
        String message
) {
}
