package com.jp.vocab.learninglink.dto;

import jakarta.validation.constraints.NotNull;

public record CreateLearningLinkRequest(
        @NotNull Long wordEntryId,
        @NotNull Long noteId,
        String source
) {
}
