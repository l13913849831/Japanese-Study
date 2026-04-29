package com.jp.vocab.backup.service;

public record BackupManifestFile(
        String path,
        BackupPayloadType payloadType,
        boolean required,
        Long recordCount
) {
    public BackupManifestFile {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (payloadType == null) {
            throw new IllegalArgumentException("payloadType must not be null");
        }
        if (recordCount != null && recordCount < 0) {
            throw new IllegalArgumentException("recordCount must not be negative");
        }
    }

    public static BackupManifestFile forPayload(BackupPayloadType payloadType, Long recordCount) {
        return new BackupManifestFile(
                payloadType.getPath(),
                payloadType,
                payloadType.isRequired(),
                recordCount
        );
    }
}
