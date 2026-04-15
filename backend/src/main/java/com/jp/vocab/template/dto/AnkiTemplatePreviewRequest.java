package com.jp.vocab.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnkiTemplatePreviewRequest(
        @NotBlank(message = "frontTemplate must not be blank")
        String frontTemplate,
        @NotBlank(message = "backTemplate must not be blank")
        String backTemplate,
        String cssTemplate,
        @Valid
        @NotNull(message = "sample must not be null")
        TemplateCardSample sample
) {
}
