package com.jp.vocab.note.dto;

public record NoteImportErrorResponse(
        Integer itemIndex,
        String field,
        String message
) {
}
