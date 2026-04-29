package com.jp.vocab.backup.service;

import com.jp.vocab.shared.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileBackupStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreReadAndDeleteLocalBackupFile() throws Exception {
        BackupProperties backupProperties = new BackupProperties();
        backupProperties.setBaseDir(tempDir.toString());
        LocalFileBackupStorage storage = new LocalFileBackupStorage(backupProperties);

        StoredBackup storedBackup = storage.store("test.zip", outputStream -> outputStream.write("payload".getBytes()));
        Path storedPath = Path.of(storedBackup.storageKey());

        assertTrue(Files.exists(storedPath));
        assertArrayEquals("payload".getBytes(), storage.readAllBytes(storedBackup));

        storage.deleteQuietly(storedBackup);

        assertFalse(Files.exists(storedPath));
    }
}
