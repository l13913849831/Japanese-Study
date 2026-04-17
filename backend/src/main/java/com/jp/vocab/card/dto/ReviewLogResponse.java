package com.jp.vocab.card.dto;

import com.jp.vocab.card.entity.ReviewLogEntity;

import java.time.OffsetDateTime;

public record ReviewLogResponse(
        Long id,
        Long cardInstanceId,
        OffsetDateTime reviewedAt,
        String rating,
        Long responseTimeMs,
        String note,
        OffsetDateTime createdAt
) {
    public static ReviewLogResponse from(ReviewLogEntity entity) {
        return new ReviewLogResponse(
                entity.getId(),
                entity.getCardInstanceId(),
                entity.getReviewedAt(),
                entity.getRating(),
                entity.getResponseTimeMs(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }
}
