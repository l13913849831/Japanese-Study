package com.jp.vocab.note.service;

import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.CardAndReviewLog;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class NoteFsrsScheduler {

    private static final long MASTERED_INTERVAL_DAYS = 21L;

    private final Scheduler scheduler = Scheduler.builder().build();

    public ScheduledNoteReview createInitialState() {
        Card card = Card.builder().build();
        OffsetDateTime dueAt = toOffsetDateTime(card.getDue());
        return new ScheduledNoteReview(card.toJson(), dueAt, "UNSTARTED", 0, null, null);
    }

    public ScheduledNoteReview review(
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
        int nextReviewCount = (currentReviewCount == null ? 0 : currentReviewCount) + 1;
        OffsetDateTime nextDueAt = toOffsetDateTime(nextCard.getDue());
        return new ScheduledNoteReview(
                nextCard.toJson(),
                nextDueAt,
                calculateMasteryStatus(nextReviewCount, nextDueAt, reviewedAt),
                nextReviewCount,
                reviewedAt,
                reviewedCard.reviewLog().toJson()
        );
    }

    String calculateMasteryStatus(int reviewCount, OffsetDateTime dueAt, OffsetDateTime reviewedAt) {
        if (reviewCount <= 0) {
            return "UNSTARTED";
        }
        if (reviewCount < 3) {
            return "LEARNING";
        }
        long intervalDays = Math.max(0L, dueAt.toLocalDate().toEpochDay() - reviewedAt.toLocalDate().toEpochDay());
        if (intervalDays >= MASTERED_INTERVAL_DAYS) {
            return "MASTERED";
        }
        return "CONSOLIDATING";
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

    public record ScheduledNoteReview(
            String fsrsCardJson,
            OffsetDateTime dueAt,
            String masteryStatus,
            Integer reviewCount,
            OffsetDateTime reviewedAt,
            String fsrsReviewLogJson
    ) {
    }
}
