package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupPackageReaderTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldReadManifestAndSnapshotFromBackupZip() throws Exception {
        BackupPackageReader reader = new BackupPackageReader(objectMapper);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new TestBackupZipWriter(objectMapper).writeMinimalZip(outputStream, BackupType.MANUAL_BACKUP);

        ParsedBackupPackage parsed = reader.read(new MockMultipartFile("file", "backup.zip", "application/zip", outputStream.toByteArray()));

        assertEquals("1", parsed.manifest().formatVersion());
        assertEquals(BackupType.MANUAL_BACKUP, parsed.manifest().backupType());
        assertEquals("Demo User", parsed.snapshot().userProfile().displayName());
    }

    @Test
    void shouldRejectZipWithoutManifest() {
        BackupPackageReader reader = new BackupPackageReader(objectMapper);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new java.util.zip.ZipEntry("data/user-profile.json"));
            zipOutputStream.write("{}".getBytes());
            zipOutputStream.closeEntry();
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
        MockMultipartFile file = new MockMultipartFile("file", "bad.zip", "application/zip", outputStream.toByteArray());

        BusinessException exception = assertThrows(BusinessException.class, () -> reader.read(file));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }
}
