package com.jp.vocab.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "displayName must not be blank")
        @Size(max = 128, message = "displayName must be at most 128 characters")
        String displayName
) {
}
