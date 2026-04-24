package com.jp.vocab.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveNoteRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @NotBlank(message = "content must not be blank")
        String content,
        List<String> tags
) {
}
