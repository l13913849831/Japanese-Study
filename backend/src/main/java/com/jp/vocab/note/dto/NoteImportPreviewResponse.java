package com.jp.vocab.note.dto;

import java.util.List;

public record NoteImportPreviewResponse(
        String splitMode,
        Integer totalItems,
        Integer readyCount,
        Integer errorCount,
        List<NoteImportPreviewItemResponse> previewItems
) {
}
