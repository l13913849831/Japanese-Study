package com.jp.vocab.note.dto;

import com.jp.vocab.note.entity.NoteEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record NoteResponse(
        Long id,
        String title,
        String content,
        List<String> tags,
        Integer reviewCount,
        String masteryStatus,
        OffsetDateTime dueAt,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static NoteResponse from(NoteEntity entity) {
        return new NoteResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getTags(),
                entity.getReviewCount(),
                entity.getMasteryStatus(),
                entity.getDueAt(),
                entity.getLastReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
