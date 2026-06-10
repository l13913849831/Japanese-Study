package com.jp.vocab.user.dto;

import java.time.OffsetDateTime;

public record MobileAuthSessionResponse(
        String token,
        OffsetDateTime expiresAt,
        CurrentUserResponse user
) {
}
