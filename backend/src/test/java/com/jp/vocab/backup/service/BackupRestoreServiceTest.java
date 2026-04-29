package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jp.vocab.backup.dto.BackupRestorePrepareResponse;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.card.repository.ReviewLogRepository;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.note.repository.NoteReviewLogRepository;
import com.jp.vocab.note.repository.NoteSourceRepository;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupRestoreServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private BackupPackageBuilder backupPackageBuilder;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserSettingRepository userSettingRepository;
    @Mock
    private WordSetRepository wordSetRepository;
    @Mock
    private WordEntryRepository wordEntryRepository;
    @Mock
    private AnkiTemplateRepository ankiTemplateRepository;
    @Mock
    private MarkdownTemplateRepository markdownTemplateRepository;
    @Mock
    private NoteSourceRepository noteSourceRepository;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private CardInstanceRepository cardInstanceRepository;
    @Mock
    private ReviewLogRepository reviewLogRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private NoteReviewLogRepository noteReviewLogRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldRequireSafetySnapshotDownloadBeforeConfirmingRestore() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new TestBackupZipWriter(objectMapper).writeMinimalZip(outputStream, BackupType.MANUAL_BACKUP);
        MockMultipartFile file = new MockMultipartFile("file", "backup.zip", "application/zip", outputStream.toByteArray());

        BackupRestoreService service = createService();
        BackupRestorePrepareResponse response = service.prepareRestore(file);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.confirmRestore(response.restoreToken())
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("Safety snapshot must be downloaded before restore", exception.getMessage());
    }

    @Test
    void shouldAllowDownloadingSafetySnapshotAfterPrepare() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
        doAnswer(invocation -> {
            java.io.OutputStream outputStream = invocation.getArgument(3);
            outputStream.write("snapshot".getBytes());
            return null;
        }).when(backupPackageBuilder).writePackage(eq(7L), eq(BackupType.SAFETY_SNAPSHOT), any(), any(java.io.OutputStream.class));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new TestBackupZipWriter(objectMapper).writeMinimalZip(outputStream, BackupType.MANUAL_BACKUP);
        MockMultipartFile file = new MockMultipartFile("file", "backup.zip", "application/zip", outputStream.toByteArray());

        BackupRestoreService service = createService();
        BackupRestorePrepareResponse response = service.prepareRestore(file);
        BackupDownload download = service.downloadSafetySnapshot(response.restoreToken());

        assertTrue(download.fileName().startsWith("safety-snapshot-"));
        assertTrue(download.fileName().endsWith(".zip"));
        assertTrue(download.content().length > 0);
        verify(backupPackageBuilder).writePackage(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(BackupType.SAFETY_SNAPSHOT), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private BackupRestoreService createService() {
        return new BackupRestoreService(
                currentUserService,
                new BackupPackageReader(objectMapper),
                backupPackageBuilder,
                new BackupRestoreSessionStore(),
                userAccountRepository,
                userSettingRepository,
                wordSetRepository,
                wordEntryRepository,
                ankiTemplateRepository,
                markdownTemplateRepository,
                noteSourceRepository,
                studyPlanRepository,
                cardInstanceRepository,
                reviewLogRepository,
                noteRepository,
                noteReviewLogRepository
        );
    }
}
