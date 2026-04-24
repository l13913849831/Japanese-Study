package com.jp.vocab.note.dto;

import java.util.List;

public record NoteImportPreviewItemResponse(
        String itemId,
        String title,
        String content,
        List<String> tags,
        String status,
        String message
) {
}
