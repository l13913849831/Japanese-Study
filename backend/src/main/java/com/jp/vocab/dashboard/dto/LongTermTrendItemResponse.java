package com.jp.vocab.dashboard.dto;

import java.time.LocalDate;

public record LongTermTrendItemResponse(
        LocalDate date,
        int wordReviews,
        int noteReviews,
        int totalReviews
) {
}
