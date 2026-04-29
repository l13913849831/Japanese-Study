package com.jp.vocab.backup.service;

import com.jp.vocab.backup.dto.BackupRestoreConfirmResponse;
import com.jp.vocab.backup.dto.BackupRestorePrepareResponse;
import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.entity.ReviewLogEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.card.repository.ReviewLogRepository;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.entity.NoteReviewLogEntity;
import com.jp.vocab.note.entity.NoteSourceEntity;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.note.repository.NoteReviewLogRepository;
import com.jp.vocab.note.repository.NoteSourceRepository;
import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import com.jp.vocab.template.entity.AnkiTemplateEntity;
import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import com.jp.vocab.wordset.entity.WordEntryEntity;
import com.jp.vocab.wordset.entity.WordSetEntity;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupRestoreService {

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final CurrentUserService currentUserService;
    private final BackupPackageReader backupPackageReader;
    private final BackupPackageBuilder backupPackageBuilder;
    private final BackupRestoreSessionStore sessionStore;
    private final UserAccountRepository userAccountRepository;
    private final UserSettingRepository userSettingRepository;
    private final WordSetRepository wordSetRepository;
    private final WordEntryRepository wordEntryRepository;
    private final AnkiTemplateRepository ankiTemplateRepository;
    private final MarkdownTemplateRepository markdownTemplateRepository;
    private final NoteSourceRepository noteSourceRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CardInstanceRepository cardInstanceRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final NoteRepository noteRepository;
    private final NoteReviewLogRepository noteReviewLogRepository;

    public BackupRestoreService(
            CurrentUserService currentUserService,
            BackupPackageReader backupPackageReader,
            BackupPackageBuilder backupPackageBuilder,
            BackupRestoreSessionStore sessionStore,
            UserAccountRepository userAccountRepository,
            UserSettingRepository userSettingRepository,
            WordSetRepository wordSetRepository,
            WordEntryRepository wordEntryRepository,
            AnkiTemplateRepository ankiTemplateRepository,
            MarkdownTemplateRepository markdownTemplateRepository,
            NoteSourceRepository noteSourceRepository,
            StudyPlanRepository studyPlanRepository,
            CardInstanceRepository cardInstanceRepository,
            ReviewLogRepository reviewLogRepository,
            NoteRepository noteRepository,
            NoteReviewLogRepository noteReviewLogRepository
    ) {
        this.currentUserService = currentUserService;
        this.backupPackageReader = backupPackageReader;
        this.backupPackageBuilder = backupPackageBuilder;
        this.sessionStore = sessionStore;
        this.userAccountRepository = userAccountRepository;
        this.userSettingRepository = userSettingRepository;
        this.wordSetRepository = wordSetRepository;
        this.wordEntryRepository = wordEntryRepository;
        this.ankiTemplateRepository = ankiTemplateRepository;
        this.markdownTemplateRepository = markdownTemplateRepository;
        this.noteSourceRepository = noteSourceRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.cardInstanceRepository = cardInstanceRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.noteRepository = noteRepository;
        this.noteReviewLogRepository = noteReviewLogRepository;
    }

    @Transactional(readOnly = true)
    public BackupRestorePrepareResponse prepareRestore(MultipartFile file) {
        Long userId = currentUserService.getCurrentUserId();
        ParsedBackupPackage backupPackage = backupPackageReader.read(file);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        byte[] safetySnapshotBytes = buildSafetySnapshot(userId, createdAt);
        String safetySnapshotFileName = buildSafetySnapshotFileName(createdAt);
        PreparedRestoreSession session = sessionStore.create(userId, backupPackage, safetySnapshotFileName, safetySnapshotBytes);
        return new BackupRestorePrepareResponse(
                session.token(),
                session.safetySnapshotFileName(),
                "/api/backups/restore/" + session.token() + "/safety-snapshot"
        );
    }

    @Transactional(readOnly = true)
    public BackupDownload downloadSafetySnapshot(String token) {
        Long userId = currentUserService.getCurrentUserId();
        PreparedRestoreSession session = sessionStore.markDownloaded(token, userId);
        return new BackupDownload(session.safetySnapshotFileName(), session.safetySnapshotBytes());
    }

    @Transactional
    public BackupRestoreConfirmResponse confirmRestore(String token) {
        Long userId = currentUserService.getCurrentUserId();
        PreparedRestoreSession session = sessionStore.getOwned(token, userId);
        if (!session.downloaded()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Safety snapshot must be downloaded before restore");
        }

        applyRestore(userId, session.backupPackage().snapshot());
        sessionStore.remove(token);
        return new BackupRestoreConfirmResponse(true);
    }

    private byte[] buildSafetySnapshot(Long userId, OffsetDateTime createdAt) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            backupPackageBuilder.writePackage(userId, BackupType.SAFETY_SNAPSHOT, createdAt, outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.EXPORT_ERROR, "Failed to generate safety snapshot");
        }
    }

    private String buildSafetySnapshotFileName(OffsetDateTime createdAt) {
        return "safety-snapshot-" + FILE_NAME_TIMESTAMP.format(createdAt) + ".zip";
    }

    private void applyRestore(Long userId, BackupPayloadSnapshot snapshot) {
        UserAccountEntity userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User account not found: " + userId));
        UserSettingEntity userSetting = userSettingRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User setting not found for user: " + userId));

        userAccount.restoreProfile(snapshot.userProfile().displayName(), snapshot.userProfile().status());
        userSetting.updatePreferredLearningOrder(snapshot.userSettings().preferredLearningOrder());

        deleteCurrentUserAssets(userId);

        Map<Long, Long> wordSetIdMap = restoreWordSets(snapshot.wordSets(), userId);
        Map<Long, Long> wordEntryIdMap = restoreWordEntries(snapshot.wordEntries(), wordSetIdMap);
        Map<Long, Long> ankiTemplateIdMap = restoreAnkiTemplates(snapshot.ankiTemplates(), userId);
        Map<Long, Long> markdownTemplateIdMap = restoreMarkdownTemplates(snapshot.markdownTemplates(), userId);
        Map<Long, Long> noteSourceIdMap = restoreNoteSources(snapshot.noteSources(), userId);
        Map<Long, Long> studyPlanIdMap = restoreStudyPlans(snapshot.studyPlans(), userId, wordSetIdMap, ankiTemplateIdMap, markdownTemplateIdMap);
        Map<Long, Long> cardInstanceIdMap = restoreCardInstances(snapshot.cardInstances(), studyPlanIdMap, wordEntryIdMap);
        Map<Long, Long> noteIdMap = restoreNotes(snapshot.notes(), userId, noteSourceIdMap);
        restoreReviewLogs(snapshot.reviewLogs(), cardInstanceIdMap);
        restoreNoteReviewLogs(snapshot.noteReviewLogs(), noteIdMap);
    }

    private void deleteCurrentUserAssets(Long userId) {
        studyPlanRepository.deleteAll(studyPlanRepository.findByUserIdOrderByIdAsc(userId));
        noteRepository.deleteAll(noteRepository.findByUserIdOrderByIdAsc(userId));
        noteSourceRepository.deleteAll(noteSourceRepository.findByOwnerUserIdOrderByIdAsc(userId));
        wordSetRepository.deleteAll(wordSetRepository.findByOwnerUserIdOrderByIdAsc(userId));
        ankiTemplateRepository.deleteAll(ankiTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId));
        markdownTemplateRepository.deleteAll(markdownTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId));
    }

    private Map<Long, Long> restoreWordSets(List<BackupWordSetPayload> payloads, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupWordSetPayload payload : payloads) {
            requireUserScope(payload.scope(), "word_set");
            WordSetEntity saved = wordSetRepository.save(
                    WordSetEntity.restoreUserOwned(
                            payload.name(),
                            payload.description(),
                            userId,
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreWordEntries(List<BackupWordEntryPayload> payloads, Map<Long, Long> wordSetIdMap) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupWordEntryPayload payload : payloads) {
            Long wordSetId = remap(wordSetIdMap, payload.wordSetId(), "word set");
            WordEntryEntity saved = wordEntryRepository.save(
                    WordEntryEntity.restore(
                            wordSetId,
                            payload.expression(),
                            payload.reading(),
                            payload.meaning(),
                            payload.partOfSpeech(),
                            payload.exampleJp(),
                            payload.exampleZh(),
                            payload.level(),
                            payload.tags(),
                            payload.sourceOrder(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreAnkiTemplates(List<BackupAnkiTemplatePayload> payloads, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupAnkiTemplatePayload payload : payloads) {
            requireUserScope(payload.scope(), "anki_template");
            AnkiTemplateEntity saved = ankiTemplateRepository.save(
                    AnkiTemplateEntity.restore(
                            payload.name(),
                            payload.description(),
                            payload.scope(),
                            userId,
                            payload.fieldMapping(),
                            payload.frontTemplate(),
                            payload.backTemplate(),
                            payload.cssTemplate(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreMarkdownTemplates(List<BackupMarkdownTemplatePayload> payloads, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupMarkdownTemplatePayload payload : payloads) {
            requireUserScope(payload.scope(), "md_template");
            MarkdownTemplateEntity saved = markdownTemplateRepository.save(
                    MarkdownTemplateEntity.restore(
                            payload.name(),
                            payload.description(),
                            payload.scope(),
                            userId,
                            payload.templateContent(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreNoteSources(List<BackupNoteSourcePayload> payloads, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupNoteSourcePayload payload : payloads) {
            requireUserScope(payload.scope(), "note_source");
            NoteSourceEntity saved = noteSourceRepository.save(
                    NoteSourceEntity.restoreUserOwned(
                            payload.title(),
                            payload.content(),
                            payload.tags(),
                            userId,
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreStudyPlans(
            List<BackupStudyPlanPayload> payloads,
            Long userId,
            Map<Long, Long> wordSetIdMap,
            Map<Long, Long> ankiTemplateIdMap,
            Map<Long, Long> markdownTemplateIdMap
    ) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupStudyPlanPayload payload : payloads) {
            StudyPlanEntity saved = studyPlanRepository.save(
                    StudyPlanEntity.restore(
                            payload.name(),
                            userId,
                            remap(wordSetIdMap, payload.wordSetId(), "word set"),
                            payload.startDate(),
                            payload.dailyNewCount(),
                            payload.reviewOffsets(),
                            remapNullable(ankiTemplateIdMap, payload.ankiTemplateId()),
                            remapNullable(markdownTemplateIdMap, payload.mdTemplateId()),
                            payload.status(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreCardInstances(
            List<BackupCardInstancePayload> payloads,
            Map<Long, Long> studyPlanIdMap,
            Map<Long, Long> wordEntryIdMap
    ) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupCardInstancePayload payload : payloads) {
            CardInstanceEntity saved = cardInstanceRepository.save(
                    CardInstanceEntity.restore(
                            remap(studyPlanIdMap, payload.planId(), "study plan"),
                            remap(wordEntryIdMap, payload.wordEntryId(), "word entry"),
                            payload.cardType(),
                            payload.sequenceNo(),
                            payload.stageNo(),
                            payload.dueAt(),
                            payload.status(),
                            payload.fsrsCardJson(),
                            payload.reviewCount(),
                            payload.lastReviewedAt(),
                            payload.weakFlag(),
                            payload.weakMarkedAt(),
                            payload.weakReviewCount(),
                            payload.lastReviewRating(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private Map<Long, Long> restoreNotes(List<BackupNotePayload> payloads, Long userId, Map<Long, Long> noteSourceIdMap) {
        Map<Long, Long> idMap = new HashMap<>();
        for (BackupNotePayload payload : payloads) {
            Long noteSourceId = remap(noteSourceIdMap, payload.noteSourceId(), "note source");
            NoteSourceEntity noteSource = noteSourceRepository.findById(noteSourceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Restored note source not found: " + noteSourceId));
            NoteEntity saved = noteRepository.save(
                    NoteEntity.restore(
                            noteSource,
                            userId,
                            payload.reviewCount(),
                            payload.masteryStatus(),
                            payload.dueAt(),
                            payload.lastReviewedAt(),
                            payload.fsrsCardJson(),
                            payload.weakFlag(),
                            payload.weakMarkedAt(),
                            payload.lastReviewRating(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
            idMap.put(payload.id(), saved.getId());
        }
        return idMap;
    }

    private void restoreReviewLogs(List<BackupReviewLogPayload> payloads, Map<Long, Long> cardInstanceIdMap) {
        for (BackupReviewLogPayload payload : payloads) {
            reviewLogRepository.save(
                    ReviewLogEntity.restore(
                            remap(cardInstanceIdMap, payload.cardInstanceId(), "card instance"),
                            payload.reviewedAt(),
                            payload.rating(),
                            payload.responseTimeMs(),
                            payload.note(),
                            payload.createdAt()
                    )
            );
        }
    }

    private void restoreNoteReviewLogs(List<BackupNoteReviewLogPayload> payloads, Map<Long, Long> noteIdMap) {
        for (BackupNoteReviewLogPayload payload : payloads) {
            noteReviewLogRepository.save(
                    NoteReviewLogEntity.restore(
                            remap(noteIdMap, payload.noteId(), "note"),
                            payload.reviewedAt(),
                            payload.rating(),
                            payload.responseTimeMs(),
                            payload.noteText(),
                            payload.fsrsReviewLogJson(),
                            payload.createdAt(),
                            payload.updatedAt()
                    )
            );
        }
    }

    private void requireUserScope(String scope, String target) {
        if (!ContentScope.USER.equals(scope)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup " + target + " scope must be USER");
        }
    }

    private Long remap(Map<Long, Long> idMap, Long oldId, String target) {
        Long newId = idMap.get(oldId);
        if (newId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "backup references missing " + target + ": " + oldId);
        }
        return newId;
    }

    private Long remapNullable(Map<Long, Long> idMap, Long oldId) {
        if (oldId == null) {
            return null;
        }
        return remap(idMap, oldId, "template");
    }
}
