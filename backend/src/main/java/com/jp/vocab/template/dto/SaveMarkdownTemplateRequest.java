package com.jp.vocab.template.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveMarkdownTemplateRequest(
        @NotBlank(message = "name must not be blank")
        String name,
        String description,
        @NotBlank(message = "templateContent must not be blank")
        String templateContent
) {
}
