package com.jp.vocab.studyplan.dto;

import com.jp.vocab.studyplan.entity.StudyPlanEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record StudyPlanResponse(
        Long id,
        String name,
        Long wordSetId,
        LocalDate startDate,
        Integer dailyNewCount,
        List<Integer> reviewOffsets,
        Long ankiTemplateId,
        Long mdTemplateId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StudyPlanResponse from(StudyPlanEntity entity) {
        return new StudyPlanResponse(
                entity.getId(),
                entity.getName(),
                entity.getWordSetId(),
                entity.getStartDate(),
                entity.getDailyNewCount(),
                entity.getReviewOffsets(),
                entity.getAnkiTemplateId(),
                entity.getMdTemplateId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
