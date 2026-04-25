package com.jp.vocab.card.dto;

import com.jp.vocab.card.entity.ReviewLogEntity;

import java.time.OffsetDateTime;

public record ReviewCardResponse(
        Long reviewId,
        Long cardId,
        String rating,
        String cardStatus,
        OffsetDateTime reviewedAt,
        boolean weak,
        OffsetDateTime weakMarkedAt,
        String todayAction
) {
    public static ReviewCardResponse from(
            ReviewLogEntity entity,
            String cardStatus,
            boolean weak,
            OffsetDateTime weakMarkedAt,
            String todayAction
    ) {
        return new ReviewCardResponse(
                entity.getId(),
                entity.getCardInstanceId(),
                entity.getRating(),
                cardStatus,
                entity.getReviewedAt(),
                weak,
                weakMarkedAt,
                todayAction
        );
    }
}
