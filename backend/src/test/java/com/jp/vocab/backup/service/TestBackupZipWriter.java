package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class TestBackupZipWriter {

    private final ObjectMapper objectMapper;

    TestBackupZipWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void writeMinimalZip(OutputStream outputStream, BackupType backupType) throws IOException {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 29, 12, 0, 0, 0, ZoneOffset.UTC);
        BackupPackageManifest manifest = BackupPackageManifest.create(
                backupType,
                now,
                BackupPackageLayout.defaultManifestFiles().stream()
                        .map(file -> BackupManifestFile.forPayload(file.payloadType(), file.payloadType() == BackupPayloadType.USER_PROFILE || file.payloadType() == BackupPayloadType.USER_SETTINGS ? 1L : 0L))
                        .toList()
        );
        BackupPayloadSnapshot snapshot = new BackupPayloadSnapshot(
                new BackupUserProfilePayload("Demo User", "ACTIVE", now, now),
                new BackupUserSettingsPayload("WORD_FIRST", now, now),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeEntry(zipOutputStream, BackupPackageLayout.MANIFEST_PATH, manifest);
            writeEntry(zipOutputStream, BackupPayloadType.USER_PROFILE.getPath(), snapshot.userProfile());
            writeEntry(zipOutputStream, BackupPayloadType.USER_SETTINGS.getPath(), snapshot.userSettings());
            writeEntry(zipOutputStream, BackupPayloadType.WORD_SETS.getPath(), snapshot.wordSets());
            writeEntry(zipOutputStream, BackupPayloadType.WORD_ENTRIES.getPath(), snapshot.wordEntries());
            writeEntry(zipOutputStream, BackupPayloadType.ANKI_TEMPLATES.getPath(), snapshot.ankiTemplates());
            writeEntry(zipOutputStream, BackupPayloadType.MARKDOWN_TEMPLATES.getPath(), snapshot.markdownTemplates());
            writeEntry(zipOutputStream, BackupPayloadType.NOTE_SOURCES.getPath(), snapshot.noteSources());
            writeEntry(zipOutputStream, BackupPayloadType.STUDY_PLANS.getPath(), snapshot.studyPlans());
            writeEntry(zipOutputStream, BackupPayloadType.CARD_INSTANCES.getPath(), snapshot.cardInstances());
            writeEntry(zipOutputStream, BackupPayloadType.REVIEW_LOGS.getPath(), snapshot.reviewLogs());
            writeEntry(zipOutputStream, BackupPayloadType.NOTES.getPath(), snapshot.notes());
            writeEntry(zipOutputStream, BackupPayloadType.NOTE_REVIEW_LOGS.getPath(), snapshot.noteReviewLogs());
        }
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String path, Object payload) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        zipOutputStream.write(objectMapper.writeValueAsBytes(payload));
        zipOutputStream.closeEntry();
    }
}
