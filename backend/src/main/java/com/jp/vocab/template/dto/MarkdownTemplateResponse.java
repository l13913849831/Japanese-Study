package com.jp.vocab.template.dto;

import com.jp.vocab.template.entity.MarkdownTemplateEntity;

import java.time.OffsetDateTime;

public record MarkdownTemplateResponse(
        Long id,
        String name,
        String description,
        String templateContent,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static MarkdownTemplateResponse from(MarkdownTemplateEntity entity) {
        return new MarkdownTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTemplateContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
