package com.jp.vocab.backup.service;

public record StoredBackup(
        String storageKey,
        String fileName,
        long size
) {
}
