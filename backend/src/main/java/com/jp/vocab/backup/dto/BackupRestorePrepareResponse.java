package com.jp.vocab.backup.dto;

public record BackupRestorePrepareResponse(
        String restoreToken,
        String safetySnapshotFileName,
        String safetySnapshotDownloadPath
) {
}
