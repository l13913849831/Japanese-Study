package com.jp.vocab.backup.service;

public record BackupDownload(
        String fileName,
        byte[] content
) {
}
