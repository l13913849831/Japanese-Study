package com.jp.vocab.exportjob.dto;

import com.jp.vocab.exportjob.entity.ExportJobEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExportJobResponse(
        Long id,
        Long planId,
        String exportType,
        LocalDate targetDate,
        String fileName,
        String filePath,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ExportJobResponse from(ExportJobEntity entity) {
        return new ExportJobResponse(
                entity.getId(),
                entity.getPlanId(),
                entity.getExportType(),
                entity.getTargetDate(),
                entity.getFileName(),
                entity.getFilePath(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
