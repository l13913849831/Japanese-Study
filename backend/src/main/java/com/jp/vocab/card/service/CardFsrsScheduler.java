package com.jp.vocab.card.service;

import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.CardAndReviewLog;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CardFsrsScheduler {

    private final Scheduler scheduler = Scheduler.builder().build();

    public InitialCardState createInitialState() {
        Card card = Card.builder().build();
        return new InitialCardState(card.toJson(), toOffsetDateTime(card.getDue()));
    }

    public ScheduledCardReview review(
            String cardJson,
            String rating,
            Integer currentReviewCount,
            OffsetDateTime reviewedAt
    ) {
        Card card = Card.fromJson(cardJson);
        CardAndReviewLog reviewedCard = scheduler.reviewCard(
                card,
                mapRating(rating),
                reviewedAt.toInstant()
        );
        Card nextCard = reviewedCard.card();
        return new ScheduledCardReview(
                nextCard.toJson(),
                toOffsetDateTime(nextCard.getDue()),
                (currentReviewCount == null ? 0 : currentReviewCount) + 1,
                reviewedAt
        );
    }

    private Rating mapRating(String rating) {
        return switch (rating) {
            case "AGAIN" -> Rating.AGAIN;
            case "HARD" -> Rating.HARD;
            case "GOOD" -> Rating.GOOD;
            case "EASY" -> Rating.EASY;
            default -> throw new IllegalArgumentException("Unsupported rating: " + rating);
        };
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    public record InitialCardState(
            String fsrsCardJson,
            OffsetDateTime dueAt
    ) {
    }

    public record ScheduledCardReview(
            String fsrsCardJson,
            OffsetDateTime dueAt,
            Integer reviewCount,
            OffsetDateTime reviewedAt
    ) {
    }
}
