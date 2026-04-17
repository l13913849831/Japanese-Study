package com.jp.vocab.card.dto;

import com.jp.vocab.card.entity.ReviewLogEntity;

import java.time.OffsetDateTime;

public record ReviewCardResponse(
        Long reviewId,
        Long cardId,
        String rating,
        String cardStatus,
        OffsetDateTime reviewedAt
) {
    public static ReviewCardResponse from(ReviewLogEntity entity, String cardStatus) {
        return new ReviewCardResponse(
                entity.getId(),
                entity.getCardInstanceId(),
                entity.getRating(),
                cardStatus,
                entity.getReviewedAt()
        );
    }
}
