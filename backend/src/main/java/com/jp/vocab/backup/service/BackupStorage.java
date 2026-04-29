package com.jp.vocab.backup.service;

import java.io.IOException;
import java.io.OutputStream;

public interface BackupStorage {

    StoredBackup store(String fileName, BackupWriter writer) throws IOException;

    byte[] readAllBytes(StoredBackup backup) throws IOException;

    void deleteQuietly(StoredBackup backup);

    @FunctionalInterface
    interface BackupWriter {
        void write(OutputStream outputStream) throws IOException;
    }
}
