package com.jp.vocab.wordset.dto;

import java.util.List;

public record WordEntryImportResponse(
        int importedCount,
        int skippedCount,
        List<WordEntryImportError> errors
) {
}
