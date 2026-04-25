package com.jp.vocab.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReviewNoteRequest(
        @NotBlank(message = "rating must not be blank")
        String rating,
        @PositiveOrZero(message = "responseTimeMs must be greater than or equal to 0")
        Long responseTimeMs,
        @PositiveOrZero(message = "sessionAgainCount must be greater than or equal to 0")
        Integer sessionAgainCount,
        @Size(max = 2000, message = "note must be at most 2000 characters")
        String note
) {
}
