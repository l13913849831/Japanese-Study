package com.jp.vocab.backup.service;

import com.jp.vocab.shared.config.BackupProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalFileBackupStorage implements BackupStorage {

    private final BackupProperties backupProperties;

    public LocalFileBackupStorage(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    @Override
    public StoredBackup store(String fileName, BackupWriter writer) throws IOException {
        Path baseDir = Path.of(backupProperties.getBaseDir());
        Files.createDirectories(baseDir);
        Path target = baseDir.resolve(UUID.randomUUID() + "-" + fileName);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            writer.write(outputStream);
        }
        return new StoredBackup(target.toAbsolutePath().toString(), fileName, Files.size(target));
    }

    @Override
    public byte[] readAllBytes(StoredBackup backup) throws IOException {
        return Files.readAllBytes(Path.of(backup.storageKey()));
    }

    @Override
    public void deleteQuietly(StoredBackup backup) {
        if (backup == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(backup.storageKey()));
        } catch (IOException ignored) {
            // Ignore cleanup failure for temporary free-tier backup files.
        }
    }
}
