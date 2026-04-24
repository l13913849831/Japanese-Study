package com.jp.vocab.note.dto;

import com.jp.vocab.note.entity.NoteReviewLogEntity;

import java.time.OffsetDateTime;

public record NoteReviewLogResponse(
        Long id,
        Long noteId,
        OffsetDateTime reviewedAt,
        String rating,
        Long responseTimeMs,
        String note,
        OffsetDateTime createdAt
) {
    public static NoteReviewLogResponse from(NoteReviewLogEntity entity) {
        return new NoteReviewLogResponse(
                entity.getId(),
                entity.getNoteId(),
                entity.getReviewedAt(),
                entity.getRating(),
                entity.getResponseTimeMs(),
                entity.getNoteText(),
                entity.getCreatedAt()
        );
    }
}
