package com.jp.vocab.note.dto;

import java.util.List;

public record NoteDashboardResponse(
        NoteDashboardOverviewResponse overview,
        List<NoteDashboardMasteryItemResponse> masteryDistribution,
        List<NoteDashboardTrendItemResponse> recentTrend,
        List<RecentNoteItemResponse> recentNotes
) {
}
