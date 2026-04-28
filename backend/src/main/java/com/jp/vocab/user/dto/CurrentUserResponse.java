package com.jp.vocab.user.dto;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String preferredLearningOrder
) {
}
