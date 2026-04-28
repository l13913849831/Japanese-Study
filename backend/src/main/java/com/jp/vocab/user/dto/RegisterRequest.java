package com.jp.vocab.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        @Size(max = 255, message = "username must be at most 255 characters")
        String username,
        @NotBlank(message = "displayName must not be blank")
        @Size(max = 128, message = "displayName must be at most 128 characters")
        String displayName,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password
) {
}
