package com.jp.vocab.user.dto;

import java.time.OffsetDateTime;

public record AdminUserListItemResponse(
        Long id,
        String username,
        String displayName,
        String status,
        String role,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
