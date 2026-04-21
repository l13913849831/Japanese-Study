package com.jp.vocab.dashboard.dto;

import java.time.LocalDate;

public record DashboardOverviewResponse(
        LocalDate date,
        int activePlanCount,
        int totalDueToday,
        int newDueToday,
        int reviewDueToday,
        int pendingDueToday,
        int reviewedToday
) {
}
