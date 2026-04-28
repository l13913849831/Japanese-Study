package com.jp.vocab.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserSettingsRequest(
        @NotBlank(message = "preferredLearningOrder must not be blank")
        String preferredLearningOrder
) {
}
