package com.jp.vocab.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, max = 72, message = "newPassword must be between 8 and 72 characters")
        String newPassword
) {
}
