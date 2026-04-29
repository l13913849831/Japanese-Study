package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupPackageBuilder {

    private final ObjectMapper objectMapper;
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

    public BackupPackageBuilder(
            ObjectMapper objectMapper,
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
        this.objectMapper = objectMapper;
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

    public void writePackage(Long userId, BackupType backupType, OffsetDateTime createdAt, OutputStream outputStream) throws IOException {
        BackupPayloadSnapshot snapshot = buildSnapshot(userId);
        List<PackageEntry> entries = buildEntries(snapshot);
        BackupPackageManifest manifest = BackupPackageManifest.create(
                backupType,
                createdAt,
                entries.stream()
                        .map(entry -> BackupManifestFile.forPayload(entry.payloadType(), entry.recordCount()))
                        .toList()
        );

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeJsonEntry(zipOutputStream, BackupPackageLayout.MANIFEST_PATH, manifest);
            for (PackageEntry entry : entries) {
                writeJsonEntry(zipOutputStream, entry.payloadType().getPath(), entry.payload());
            }
        }
    }

    private BackupPayloadSnapshot buildSnapshot(Long userId) {
        UserAccountEntity userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User account not found: " + userId));
        UserSettingEntity userSetting = userSettingRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User setting not found for user: " + userId));

        List<WordSetEntity> wordSets = wordSetRepository.findByOwnerUserIdOrderByIdAsc(userId);
        List<Long> wordSetIds = wordSets.stream().map(WordSetEntity::getId).toList();
        List<WordEntryEntity> wordEntries = wordSetIds.isEmpty()
                ? List.of()
                : wordEntryRepository.findByWordSetIdInOrderByWordSetIdAscSourceOrderAscIdAsc(wordSetIds);

        List<AnkiTemplateEntity> ankiTemplates = ankiTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId);
        List<MarkdownTemplateEntity> markdownTemplates = markdownTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId);
        List<NoteSourceEntity> noteSources = noteSourceRepository.findByOwnerUserIdOrderByIdAsc(userId);
        List<StudyPlanEntity> studyPlans = studyPlanRepository.findByUserIdOrderByIdAsc(userId);
        List<Long> planIds = studyPlans.stream().map(StudyPlanEntity::getId).toList();
        List<CardInstanceEntity> cardInstances = planIds.isEmpty()
                ? List.of()
                : cardInstanceRepository.findByPlanIdInOrderByPlanIdAscSequenceNoAscStageNoAscIdAsc(planIds);
        List<Long> cardInstanceIds = cardInstances.stream().map(CardInstanceEntity::getId).toList();
        List<ReviewLogEntity> reviewLogs = cardInstanceIds.isEmpty()
                ? List.of()
                : reviewLogRepository.findByCardInstanceIdInOrderByCardInstanceIdAscReviewedAtAscIdAsc(cardInstanceIds);
        List<NoteEntity> notes = noteRepository.findByUserIdOrderByIdAsc(userId);
        List<Long> noteIds = notes.stream().map(NoteEntity::getId).toList();
        List<NoteReviewLogEntity> noteReviewLogs = noteIds.isEmpty()
                ? List.of()
                : noteReviewLogRepository.findByNoteIdInOrderByNoteIdAscReviewedAtAscIdAsc(noteIds);

        return new BackupPayloadSnapshot(
                new BackupUserProfilePayload(
                        userAccount.getDisplayName(),
                        userAccount.getStatus(),
                        userAccount.getCreatedAt(),
                        userAccount.getUpdatedAt()
                ),
                new BackupUserSettingsPayload(
                        userSetting.getPreferredLearningOrder(),
                        userSetting.getCreatedAt(),
                        userSetting.getUpdatedAt()
                ),
                wordSets.stream().map(this::toWordSetPayload).toList(),
                wordEntries.stream().map(this::toWordEntryPayload).toList(),
                ankiTemplates.stream().map(this::toAnkiTemplatePayload).toList(),
                markdownTemplates.stream().map(this::toMarkdownTemplatePayload).toList(),
                noteSources.stream().map(this::toNoteSourcePayload).toList(),
                studyPlans.stream().map(this::toStudyPlanPayload).toList(),
                cardInstances.stream().map(this::toCardInstancePayload).toList(),
                reviewLogs.stream().map(this::toReviewLogPayload).toList(),
                notes.stream().map(this::toNotePayload).toList(),
                noteReviewLogs.stream().map(this::toNoteReviewLogPayload).toList()
        );
    }

    private List<PackageEntry> buildEntries(BackupPayloadSnapshot snapshot) {
        List<PackageEntry> entries = new ArrayList<>();
        entries.add(new PackageEntry(BackupPayloadType.USER_PROFILE, snapshot.userProfile(), 1L));
        entries.add(new PackageEntry(BackupPayloadType.USER_SETTINGS, snapshot.userSettings(), 1L));
        entries.add(new PackageEntry(BackupPayloadType.WORD_SETS, snapshot.wordSets(), count(snapshot.wordSets())));
        entries.add(new PackageEntry(BackupPayloadType.WORD_ENTRIES, snapshot.wordEntries(), count(snapshot.wordEntries())));
        entries.add(new PackageEntry(BackupPayloadType.ANKI_TEMPLATES, snapshot.ankiTemplates(), count(snapshot.ankiTemplates())));
        entries.add(new PackageEntry(BackupPayloadType.MARKDOWN_TEMPLATES, snapshot.markdownTemplates(), count(snapshot.markdownTemplates())));
        entries.add(new PackageEntry(BackupPayloadType.NOTE_SOURCES, snapshot.noteSources(), count(snapshot.noteSources())));
        entries.add(new PackageEntry(BackupPayloadType.STUDY_PLANS, snapshot.studyPlans(), count(snapshot.studyPlans())));
        entries.add(new PackageEntry(BackupPayloadType.CARD_INSTANCES, snapshot.cardInstances(), count(snapshot.cardInstances())));
        entries.add(new PackageEntry(BackupPayloadType.REVIEW_LOGS, snapshot.reviewLogs(), count(snapshot.reviewLogs())));
        entries.add(new PackageEntry(BackupPayloadType.NOTES, snapshot.notes(), count(snapshot.notes())));
        entries.add(new PackageEntry(BackupPayloadType.NOTE_REVIEW_LOGS, snapshot.noteReviewLogs(), count(snapshot.noteReviewLogs())));
        return entries;
    }

    private void writeJsonEntry(ZipOutputStream zipOutputStream, String path, Object payload) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        zipOutputStream.write(objectMapper.writeValueAsBytes(payload));
        zipOutputStream.closeEntry();
    }

    private long count(List<?> items) {
        return items.size();
    }

    private BackupWordSetPayload toWordSetPayload(WordSetEntity entity) {
        return new BackupWordSetPayload(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getScope(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupWordEntryPayload toWordEntryPayload(WordEntryEntity entity) {
        return new BackupWordEntryPayload(
                entity.getId(),
                entity.getWordSetId(),
                entity.getExpression(),
                entity.getReading(),
                entity.getMeaning(),
                entity.getPartOfSpeech(),
                entity.getExampleJp(),
                entity.getExampleZh(),
                entity.getLevel(),
                entity.getTags(),
                entity.getSourceOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupAnkiTemplatePayload toAnkiTemplatePayload(AnkiTemplateEntity entity) {
        return new BackupAnkiTemplatePayload(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getScope(),
                entity.getFieldMapping(),
                entity.getFrontTemplate(),
                entity.getBackTemplate(),
                entity.getCssTemplate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupMarkdownTemplatePayload toMarkdownTemplatePayload(MarkdownTemplateEntity entity) {
        return new BackupMarkdownTemplatePayload(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getScope(),
                entity.getTemplateContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupNoteSourcePayload toNoteSourcePayload(NoteSourceEntity entity) {
        return new BackupNoteSourcePayload(
                entity.getId(),
                entity.getScope(),
                entity.getTitle(),
                entity.getContent(),
                entity.getTags(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupStudyPlanPayload toStudyPlanPayload(StudyPlanEntity entity) {
        return new BackupStudyPlanPayload(
                entity.getId(),
                entity.getName(),
                entity.getWordSetId(),
                entity.getStartDate(),
                entity.getDailyNewCount(),
                entity.getReviewOffsets(),
                entity.getAnkiTemplateId(),
                entity.getMdTemplateId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupCardInstancePayload toCardInstancePayload(CardInstanceEntity entity) {
        return new BackupCardInstancePayload(
                entity.getId(),
                entity.getPlanId(),
                entity.getWordEntryId(),
                entity.getCardType(),
                entity.getSequenceNo(),
                entity.getStageNo(),
                entity.getDueDate(),
                entity.getDueAt(),
                entity.getStatus(),
                entity.getFsrsCardJson(),
                entity.getReviewCount(),
                entity.getLastReviewedAt(),
                entity.isWeakFlag(),
                entity.getWeakMarkedAt(),
                entity.getWeakReviewCount(),
                entity.getLastReviewRating(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupReviewLogPayload toReviewLogPayload(ReviewLogEntity entity) {
        return new BackupReviewLogPayload(
                entity.getId(),
                entity.getCardInstanceId(),
                entity.getReviewedAt(),
                entity.getRating(),
                entity.getResponseTimeMs(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    private BackupNotePayload toNotePayload(NoteEntity entity) {
        return new BackupNotePayload(
                entity.getId(),
                entity.getNoteSourceId(),
                entity.getReviewCount(),
                entity.getMasteryStatus(),
                entity.getDueAt(),
                entity.getLastReviewedAt(),
                entity.getFsrsCardJson(),
                entity.isWeakFlag(),
                entity.getWeakMarkedAt(),
                entity.getLastReviewRating(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BackupNoteReviewLogPayload toNoteReviewLogPayload(NoteReviewLogEntity entity) {
        return new BackupNoteReviewLogPayload(
                entity.getId(),
                entity.getNoteId(),
                entity.getReviewedAt(),
                entity.getRating(),
                entity.getResponseTimeMs(),
                entity.getNoteText(),
                entity.getFsrsReviewLogJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private record PackageEntry(
            BackupPayloadType payloadType,
            Object payload,
            Long recordCount
    ) {
    }
}
