package com.jp.vocab.user.dto;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String preferredLearningOrder,
        List<String> roles
) {
}
