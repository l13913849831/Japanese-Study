package com.jp.vocab.weakitem.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record WeakNoteItemResponse(
        Long noteId,
        String title,
        List<String> tags,
        String masteryStatus,
        String lastReviewRating,
        OffsetDateTime weakMarkedAt
) {
}
