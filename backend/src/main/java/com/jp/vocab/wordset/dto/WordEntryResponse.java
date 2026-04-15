package com.jp.vocab.wordset.dto;

import com.jp.vocab.wordset.entity.WordEntryEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record WordEntryResponse(
        Long id,
        Long wordSetId,
        String expression,
        String reading,
        String meaning,
        String partOfSpeech,
        String exampleJp,
        String exampleZh,
        String level,
        List<String> tags,
        Integer sourceOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static WordEntryResponse from(WordEntryEntity entity) {
        return new WordEntryResponse(
                entity.getId(),
                entity.getWordSetId(),
                entity.getExpression(),
                entity.getReading(),
                entity.getMeaning(),
                entity.getPartOfSpeech(),
                entity.getExampleJp(),
                entity.getExampleZh(),
                entity.getLevel(),
                entity.getTags(),
                entity.getSourceOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
