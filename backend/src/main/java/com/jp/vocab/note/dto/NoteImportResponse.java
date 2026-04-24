package com.jp.vocab.note.dto;

import java.util.List;

public record NoteImportResponse(
        Integer importedCount,
        Integer skippedCount,
        List<NoteImportErrorResponse> errors
) {
}
