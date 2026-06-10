package com.jp.vocab.user.dto;

import java.time.OffsetDateTime;

public record SecurityAuditEventResponse(
        Long id,
        String eventType,
        String outcome,
        Long userId,
        String username,
        String ipAddress,
        String userAgent,
        String message,
        OffsetDateTime createdAt
) {
}
