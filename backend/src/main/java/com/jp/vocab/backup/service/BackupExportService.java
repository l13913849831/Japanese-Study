package com.jp.vocab.backup.service;

import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class BackupExportService {

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final CurrentUserService currentUserService;
    private final BackupPackageBuilder backupPackageBuilder;
    private final BackupStorage backupStorage;

    public BackupExportService(
            CurrentUserService currentUserService,
            BackupPackageBuilder backupPackageBuilder,
            BackupStorage backupStorage
    ) {
        this.currentUserService = currentUserService;
        this.backupPackageBuilder = backupPackageBuilder;
        this.backupStorage = backupStorage;
    }

    @Transactional(readOnly = true)
    public BackupDownload exportCurrentAccountBackup() {
        Long userId = currentUserService.getCurrentUserId();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        String fileName = buildFileName(createdAt);
        StoredBackup storedBackup = null;
        try {
            storedBackup = backupStorage.store(
                    fileName,
                    outputStream -> backupPackageBuilder.writePackage(userId, BackupType.MANUAL_BACKUP, createdAt, outputStream)
            );
            return new BackupDownload(fileName, backupStorage.readAllBytes(storedBackup));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.EXPORT_ERROR, "Failed to generate backup package");
        } finally {
            backupStorage.deleteQuietly(storedBackup);
        }
    }

    private String buildFileName(OffsetDateTime createdAt) {
        return "account-backup-" + FILE_NAME_TIMESTAMP.format(createdAt) + ".zip";
    }
}
