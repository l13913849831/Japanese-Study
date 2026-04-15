package com.jp.vocab.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MarkdownTemplatePreviewRequest(
        @NotBlank(message = "templateContent must not be blank")
        String templateContent,
        @NotBlank(message = "date must not be blank")
        String date,
        @NotBlank(message = "planName must not be blank")
        String planName,
        @Valid
        @NotNull(message = "newCards must not be null")
        List<TemplateCardSample> newCards,
        @Valid
        @NotNull(message = "reviewCards must not be null")
        List<TemplateCardSample> reviewCards
) {
}
