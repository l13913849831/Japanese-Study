package com.jp.vocab.wordset.dto;

import com.jp.vocab.wordset.entity.WordSetEntity;

import java.time.OffsetDateTime;

public record WordSetResponse(
        Long id,
        String name,
        String description,
        String scope,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static WordSetResponse from(WordSetEntity entity) {
        return new WordSetResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getScope(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
