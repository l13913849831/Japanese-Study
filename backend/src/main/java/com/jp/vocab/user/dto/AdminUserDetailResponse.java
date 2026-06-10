package com.jp.vocab.user.dto;

import java.time.OffsetDateTime;

public record AdminUserDetailResponse(
        Long id,
        String username,
        String displayName,
        String status,
        String role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        AdminUserAssetSummaryResponse assetSummary
) {
}
