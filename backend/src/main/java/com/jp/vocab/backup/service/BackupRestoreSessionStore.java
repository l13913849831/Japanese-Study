package com.jp.vocab.backup.service;

import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BackupRestoreSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, PreparedRestoreSession> sessions = new ConcurrentHashMap<>();

    public PreparedRestoreSession create(Long userId, ParsedBackupPackage backupPackage, String safetySnapshotFileName, byte[] safetySnapshotBytes) {
        cleanupExpired();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PreparedRestoreSession session = new PreparedRestoreSession(
                UUID.randomUUID().toString(),
                userId,
                backupPackage,
                safetySnapshotFileName,
                safetySnapshotBytes,
                now,
                now.plus(SESSION_TTL),
                false
        );
        sessions.put(session.token(), session);
        return session;
    }

    public PreparedRestoreSession getOwned(String token, Long userId) {
        cleanupExpired();
        PreparedRestoreSession session = sessions.get(token);
        if (session == null || !session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Restore session not found: " + token);
        }
        return session;
    }

    public PreparedRestoreSession markDownloaded(String token, Long userId) {
        PreparedRestoreSession session = getOwned(token, userId);
        PreparedRestoreSession updated = session.markDownloaded();
        sessions.put(token, updated);
        return updated;
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    private void cleanupExpired() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
