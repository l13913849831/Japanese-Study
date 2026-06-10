package com.jp.vocab.user.dto;

public record AdminUserAssetSummaryResponse(
        long wordSetCount,
        long studyPlanCount,
        long noteCount
) {
}
