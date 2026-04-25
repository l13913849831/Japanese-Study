package com.jp.vocab.weakitem.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record WeakWordItemResponse(
        Long cardId,
        Long planId,
        String planName,
        Long wordEntryId,
        String expression,
        String reading,
        String meaning,
        LocalDate dueDate,
        String lastReviewRating,
        OffsetDateTime weakMarkedAt
) {
}
