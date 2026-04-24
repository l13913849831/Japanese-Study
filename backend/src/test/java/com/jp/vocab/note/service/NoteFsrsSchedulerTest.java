package com.jp.vocab.note.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteFsrsSchedulerTest {

    private final NoteFsrsScheduler scheduler = new NoteFsrsScheduler();

    @Test
    void shouldMarkUntouchedNoteAsUnstarted() {
        NoteFsrsScheduler.ScheduledNoteReview initialState = scheduler.createInitialState();
        assertEquals("UNSTARTED", initialState.masteryStatus());
        assertEquals(0, initialState.reviewCount());
    }

    @Test
    void shouldPromoteLongIntervalReviewToMastered() {
        String masteryStatus = scheduler.calculateMasteryStatus(
                3,
                OffsetDateTime.of(2026, 5, 30, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );
        assertEquals("MASTERED", masteryStatus);
    }
}
