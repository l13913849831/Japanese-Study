package com.jp.vocab.wordset.dto;

import java.util.List;

public record WordEntryImportPreviewResponse(
        String sourceType,
        int totalRows,
        int readyCount,
        int duplicateCount,
        int errorCount,
        List<WordEntryImportFieldMapping> fieldMappings,
        List<WordEntryImportPreviewRow> previewRows
) {
}
