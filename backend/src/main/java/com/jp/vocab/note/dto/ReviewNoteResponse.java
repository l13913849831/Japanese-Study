package com.jp.vocab.note.dto;

import com.jp.vocab.note.entity.NoteReviewLogEntity;

import java.time.OffsetDateTime;

public record ReviewNoteResponse(
        Long reviewId,
        Long noteId,
        String rating,
        String masteryStatus,
        OffsetDateTime reviewedAt,
        OffsetDateTime dueAt
) {
    public static ReviewNoteResponse from(NoteReviewLogEntity entity, String masteryStatus, OffsetDateTime dueAt) {
        return new ReviewNoteResponse(
                entity.getId(),
                entity.getNoteId(),
                entity.getRating(),
                masteryStatus,
                entity.getReviewedAt(),
                dueAt
        );
    }
}
