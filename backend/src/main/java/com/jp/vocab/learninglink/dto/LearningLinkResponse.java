package com.jp.vocab.learninglink.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LearningLinkResponse(
        Long linkId,
        Long wordEntryId,
        String expression,
        String reading,
        Long noteId,
        String noteTitle,
        List<String> noteTags,
        String source,
        OffsetDateTime createdAt
) {
}
