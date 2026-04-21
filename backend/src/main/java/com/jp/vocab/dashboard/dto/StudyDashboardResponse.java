package com.jp.vocab.dashboard.dto;

import java.util.List;

public record StudyDashboardResponse(
        DashboardOverviewResponse overview,
        List<DashboardPlanSummaryResponse> activePlans,
        List<DashboardTrendItemResponse> recentTrend
) {
}
