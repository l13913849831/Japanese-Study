package com.jp.vocab.wordset.dto;

public record WordEntryImportFieldMapping(
        String targetField,
        boolean required,
        boolean mapped,
        String sourceField,
        String note
) {
}
