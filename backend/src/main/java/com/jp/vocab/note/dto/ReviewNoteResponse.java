package com.jp.vocab.note.dto;

import com.jp.vocab.note.entity.NoteReviewLogEntity;

import java.time.OffsetDateTime;

public record ReviewNoteResponse(
        Long reviewId,
        Long noteId,
        String rating,
        String masteryStatus,
        OffsetDateTime reviewedAt,
        OffsetDateTime dueAt,
        boolean weak,
        OffsetDateTime weakMarkedAt,
        String todayAction
) {
    public static ReviewNoteResponse from(
            NoteReviewLogEntity entity,
            String masteryStatus,
            OffsetDateTime dueAt,
            boolean weak,
            OffsetDateTime weakMarkedAt,
            String todayAction
    ) {
        return new ReviewNoteResponse(
                entity.getId(),
                entity.getNoteId(),
                entity.getRating(),
                masteryStatus,
                entity.getReviewedAt(),
                dueAt,
                weak,
                weakMarkedAt,
                todayAction
        );
    }
}
