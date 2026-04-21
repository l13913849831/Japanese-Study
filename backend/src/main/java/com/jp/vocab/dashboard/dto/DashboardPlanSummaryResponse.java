package com.jp.vocab.dashboard.dto;

import java.time.LocalDate;

public record DashboardPlanSummaryResponse(
        Long planId,
        String planName,
        String status,
        LocalDate startDate,
        int dailyNewCount,
        int totalCards,
        int completedCards,
        double completionRate,
        int dueToday,
        int newToday,
        int reviewToday,
        int pendingToday,
        int reviewedToday
) {
}
