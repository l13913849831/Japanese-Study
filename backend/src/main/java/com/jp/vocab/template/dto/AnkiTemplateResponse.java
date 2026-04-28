package com.jp.vocab.template.dto;

import com.jp.vocab.template.entity.AnkiTemplateEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AnkiTemplateResponse(
        Long id,
        String name,
        String description,
        String scope,
        Map<String, List<String>> fieldMapping,
        String frontTemplate,
        String backTemplate,
        String cssTemplate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AnkiTemplateResponse from(AnkiTemplateEntity entity) {
        return new AnkiTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getScope(),
                entity.getFieldMapping(),
                entity.getFrontTemplate(),
                entity.getBackTemplate(),
                entity.getCssTemplate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
