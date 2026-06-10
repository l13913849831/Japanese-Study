package com.jp.vocab.dashboard.dto;

import java.util.List;

public record LongTermDashboardResponse(
        LongTermLearningSummaryResponse summary,
        List<LongTermTrendItemResponse> trend,
        LongTermLoadForecastResponse loadForecast
) {
}
