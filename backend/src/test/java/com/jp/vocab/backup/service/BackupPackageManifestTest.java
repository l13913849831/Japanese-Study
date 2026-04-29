package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupPackageManifestTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldExposeStableDefaultFileLayout() {
        List<BackupManifestFile> files = BackupPackageLayout.defaultManifestFiles();

        assertEquals(BackupPayloadType.values().length, files.size());
        assertEquals("manifest.json", BackupPackageLayout.MANIFEST_PATH);
        assertIterableEquals(
                List.of(
                        "data/user-profile.json",
                        "data/user-settings.json",
                        "data/word-sets.json",
                        "data/word-entries.json",
                        "data/anki-templates.json",
                        "data/markdown-templates.json",
                        "data/note-sources.json",
                        "data/study-plans.json",
                        "data/card-instances.json",
                        "data/review-logs.json",
                        "data/notes.json",
                        "data/note-review-logs.json"
                ),
                files.stream().map(BackupManifestFile::path).toList()
        );
    }

    @Test
    void shouldCreateManifestWithStableProtocolDefaults() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 4, 29, 12, 0, 0, 0, ZoneOffset.UTC);
        BackupPackageManifest manifest = BackupPackageManifest.create(
                BackupType.MANUAL_BACKUP,
                createdAt,
                BackupPackageLayout.defaultManifestFiles()
        );

        String json = objectMapper.writeValueAsString(manifest);

        assertTrue(json.contains("\"formatVersion\":\"1\""));
        assertTrue(json.contains("\"packageType\":\"ACCOUNT_BACKUP\""));
        assertTrue(json.contains("\"backupType\":\"MANUAL_BACKUP\""));
        assertTrue(json.contains("\"scope\":\"CURRENT_ACCOUNT_LEARNING_ASSETS\""));
        assertTrue(json.contains("\"createdAt\":\"2026-04-29T12:00:00Z\""));
        assertTrue(json.contains("\"payloadType\":\"USER_PROFILE\""));
    }

    @Test
    void shouldRejectNegativeRecordCount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BackupManifestFile("data/user-profile.json", BackupPayloadType.USER_PROFILE, true, -1L)
        );

        assertEquals("recordCount must not be negative", exception.getMessage());
    }

    @Test
    void shouldDefensivelyCopyManifestFiles() {
        BackupManifestFile file = BackupManifestFile.forPayload(BackupPayloadType.USER_PROFILE, 1L);
        List<BackupManifestFile> files = new java.util.ArrayList<>(List.of(file));

        BackupPackageManifest manifest = BackupPackageManifest.create(
                BackupType.SAFETY_SNAPSHOT,
                OffsetDateTime.now(ZoneOffset.UTC),
                files
        );
        files.clear();

        assertEquals(1, manifest.files().size());
        assertEquals(file, manifest.files().getFirst());
    }
}
