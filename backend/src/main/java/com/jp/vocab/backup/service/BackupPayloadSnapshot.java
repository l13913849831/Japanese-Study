package com.jp.vocab.backup.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

record BackupPayloadSnapshot(
        BackupUserProfilePayload userProfile,
        BackupUserSettingsPayload userSettings,
        List<BackupWordSetPayload> wordSets,
        List<BackupWordEntryPayload> wordEntries,
        List<BackupAnkiTemplatePayload> ankiTemplates,
        List<BackupMarkdownTemplatePayload> markdownTemplates,
        List<BackupNoteSourcePayload> noteSources,
        List<BackupStudyPlanPayload> studyPlans,
        List<BackupCardInstancePayload> cardInstances,
        List<BackupReviewLogPayload> reviewLogs,
        List<BackupNotePayload> notes,
        List<BackupNoteReviewLogPayload> noteReviewLogs
) {
}

record BackupUserProfilePayload(
        String displayName,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupUserSettingsPayload(
        String preferredLearningOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupWordSetPayload(
        Long id,
        String name,
        String description,
        String scope,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupWordEntryPayload(
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
}

record BackupAnkiTemplatePayload(
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
}

record BackupMarkdownTemplatePayload(
        Long id,
        String name,
        String description,
        String scope,
        String templateContent,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupNoteSourcePayload(
        Long id,
        String scope,
        String title,
        String content,
        List<String> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupStudyPlanPayload(
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
}

record BackupCardInstancePayload(
        Long id,
        Long planId,
        Long wordEntryId,
        String cardType,
        Integer sequenceNo,
        Integer stageNo,
        LocalDate dueDate,
        OffsetDateTime dueAt,
        String status,
        String fsrsCardJson,
        Integer reviewCount,
        OffsetDateTime lastReviewedAt,
        boolean weakFlag,
        OffsetDateTime weakMarkedAt,
        Integer weakReviewCount,
        String lastReviewRating,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupReviewLogPayload(
        Long id,
        Long cardInstanceId,
        OffsetDateTime reviewedAt,
        String rating,
        Long responseTimeMs,
        String note,
        OffsetDateTime createdAt
) {
}

record BackupNotePayload(
        Long id,
        Long noteSourceId,
        Integer reviewCount,
        String masteryStatus,
        OffsetDateTime dueAt,
        OffsetDateTime lastReviewedAt,
        String fsrsCardJson,
        boolean weakFlag,
        OffsetDateTime weakMarkedAt,
        String lastReviewRating,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

record BackupNoteReviewLogPayload(
        Long id,
        Long noteId,
        OffsetDateTime reviewedAt,
        String rating,
        Long responseTimeMs,
        String noteText,
        String fsrsReviewLogJson,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
