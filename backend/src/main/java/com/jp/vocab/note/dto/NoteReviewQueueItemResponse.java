package com.jp.vocab.note.dto;

import com.jp.vocab.note.entity.NoteEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record NoteReviewQueueItemResponse(
        Long id,
        String title,
        String content,
        List<String> tags,
        String masteryStatus,
        Integer reviewCount,
        OffsetDateTime dueAt,
        OffsetDateTime lastReviewedAt
) {
    public static NoteReviewQueueItemResponse from(NoteEntity entity) {
        return new NoteReviewQueueItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getTags(),
                entity.getMasteryStatus(),
                entity.getReviewCount(),
                entity.getDueAt(),
                entity.getLastReviewedAt()
        );
    }
}
