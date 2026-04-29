package com.jp.vocab.backup.service;

import java.time.OffsetDateTime;

record PreparedRestoreSession(
        String token,
        Long userId,
        ParsedBackupPackage backupPackage,
        String safetySnapshotFileName,
        byte[] safetySnapshotBytes,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        boolean downloaded
) {
    PreparedRestoreSession markDownloaded() {
        return new PreparedRestoreSession(
                token,
                userId,
                backupPackage,
                safetySnapshotFileName,
                safetySnapshotBytes,
                createdAt,
                expiresAt,
                true
        );
    }
}
