package com.jp.vocab.note.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RecentNoteItemResponse(
        Long id,
        String title,
        List<String> tags,
        String masteryStatus,
        OffsetDateTime createdAt
) {
}
