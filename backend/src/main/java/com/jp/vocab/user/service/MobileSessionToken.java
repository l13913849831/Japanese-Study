package com.jp.vocab.user.service;

import java.time.OffsetDateTime;

public record MobileSessionToken(
        String token,
        OffsetDateTime expiresAt
) {
}
