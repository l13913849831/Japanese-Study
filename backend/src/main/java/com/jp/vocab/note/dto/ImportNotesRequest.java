package com.jp.vocab.note.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportNotesRequest(
        @NotEmpty(message = "items must not be empty")
        List<@Valid ImportNotesRequestItem> items
) {
}
