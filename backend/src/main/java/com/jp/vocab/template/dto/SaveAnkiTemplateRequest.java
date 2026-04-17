package com.jp.vocab.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SaveAnkiTemplateRequest(
        @NotBlank(message = "name must not be blank")
        String name,
        String description,
        @NotNull(message = "fieldMapping must not be null")
        Map<String, List<String>> fieldMapping,
        @NotBlank(message = "frontTemplate must not be blank")
        String frontTemplate,
        @NotBlank(message = "backTemplate must not be blank")
        String backTemplate,
        String cssTemplate
) {
}
