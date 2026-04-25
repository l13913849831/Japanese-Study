package com.jp.vocab.card.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardFsrsSchedulerTest {

    private final CardFsrsScheduler scheduler = new CardFsrsScheduler();

    @Test
    void shouldCreateInitialFsrsState() {
        CardFsrsScheduler.InitialCardState initialState = scheduler.createInitialState();

        assertNotNull(initialState.fsrsCardJson());
        assertNotNull(initialState.dueAt());
    }

    @Test
    void shouldAdvanceReviewCountAfterReview() {
        CardFsrsScheduler.InitialCardState initialState = scheduler.createInitialState();

        CardFsrsScheduler.ScheduledCardReview scheduled = scheduler.review(
                initialState.fsrsCardJson(),
                "GOOD",
                0,
                OffsetDateTime.of(2026, 4, 25, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        assertEquals(1, scheduled.reviewCount());
        assertNotNull(scheduled.fsrsCardJson());
        assertTrue(scheduled.dueAt().isAfter(scheduled.reviewedAt()) || scheduled.dueAt().isEqual(scheduled.reviewedAt()));
    }
}
