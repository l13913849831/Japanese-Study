package com.jp.vocab.wordset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWordSetRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name must be less than or equal to 128 characters")
        String name,

        @Size(max = 512, message = "description must be less than or equal to 512 characters")
        String description
) {
}
