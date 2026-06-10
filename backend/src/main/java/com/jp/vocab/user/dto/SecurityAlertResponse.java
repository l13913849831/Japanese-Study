package com.jp.vocab.user.dto;

import java.time.OffsetDateTime;

public record SecurityAlertResponse(
        String id,
        String alertType,
        String severity,
        String title,
        String description,
        String username,
        String ipAddress,
        long eventCount,
        OffsetDateTime lastSeenAt
) {
}
