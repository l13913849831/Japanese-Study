package com.jp.vocab.backup.service;

import com.jp.vocab.shared.auth.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupExportServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private BackupPackageBuilder backupPackageBuilder;

    @Test
    void shouldDeleteStoredTempFileAfterReturningBackupBytes() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        TrackingBackupStorage storage = new TrackingBackupStorage();
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(3);
            outputStream.write("zip-data".getBytes());
            return null;
        }).when(backupPackageBuilder).writePackage(eq(7L), eq(BackupType.MANUAL_BACKUP), any(), any(OutputStream.class));

        BackupExportService service = new BackupExportService(currentUserService, backupPackageBuilder, storage);

        BackupDownload download = service.exportCurrentAccountBackup();

        assertTrue(download.fileName().startsWith("account-backup-"));
        assertTrue(download.fileName().endsWith(".zip"));
        assertArrayEquals("zip-data".getBytes(), download.content());
        assertTrue(storage.deleted);
        verify(backupPackageBuilder).writePackage(eq(7L), eq(BackupType.MANUAL_BACKUP), any(), any(OutputStream.class));
    }

    private static class TrackingBackupStorage implements BackupStorage {
        private final Map<String, byte[]> store = new HashMap<>();
        private boolean deleted;

        @Override
        public StoredBackup store(String fileName, BackupWriter writer) throws IOException {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            writer.write(outputStream);
            store.put(fileName, outputStream.toByteArray());
            return new StoredBackup(fileName, fileName, outputStream.size());
        }

        @Override
        public byte[] readAllBytes(StoredBackup backup) {
            return store.get(backup.storageKey());
        }

        @Override
        public void deleteQuietly(StoredBackup backup) {
            deleted = true;
            store.remove(backup.storageKey());
        }
    }
}
