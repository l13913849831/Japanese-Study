package com.jp.vocab.dashboard.dto;

import java.time.LocalDate;

public record LongTermLearningSummaryResponse(
        LocalDate date,
        int rangeDays,
        int currentStreakDays,
        int longestStreakDays,
        int reviewedLast7Days,
        int wordReviewedLast7Days,
        int noteReviewedLast7Days,
        int reviewedLast30Days,
        int wordReviewedLast30Days,
        int noteReviewedLast30Days
) {
}
