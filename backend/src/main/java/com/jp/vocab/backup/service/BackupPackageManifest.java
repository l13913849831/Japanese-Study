package com.jp.vocab.backup.service;

import java.time.OffsetDateTime;
import java.util.List;

public record BackupPackageManifest(
        String formatVersion,
        String packageType,
        BackupType backupType,
        String scope,
        OffsetDateTime createdAt,
        List<BackupManifestFile> files
) {
    public static final String FORMAT_VERSION = "1";
    public static final String PACKAGE_TYPE = "ACCOUNT_BACKUP";
    public static final String SCOPE = "CURRENT_ACCOUNT_LEARNING_ASSETS";

    public BackupPackageManifest {
        if (formatVersion == null || formatVersion.isBlank()) {
            throw new IllegalArgumentException("formatVersion must not be blank");
        }
        if (packageType == null || packageType.isBlank()) {
            throw new IllegalArgumentException("packageType must not be blank");
        }
        if (backupType == null) {
            throw new IllegalArgumentException("backupType must not be null");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files must not be empty");
        }
        files = List.copyOf(files);
    }

    public static BackupPackageManifest create(BackupType backupType, OffsetDateTime createdAt, List<BackupManifestFile> files) {
        return new BackupPackageManifest(
                FORMAT_VERSION,
                PACKAGE_TYPE,
                backupType,
                SCOPE,
                createdAt,
                files
        );
    }
}
