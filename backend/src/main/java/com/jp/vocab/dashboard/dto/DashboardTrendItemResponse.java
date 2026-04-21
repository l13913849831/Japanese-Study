package com.jp.vocab.dashboard.dto;

import java.time.LocalDate;

public record DashboardTrendItemResponse(
        LocalDate date,
        int newCards,
        int reviewCards,
        int reviewedCards
) {
}
