package com.jp.vocab.backup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupPackageBuilderTest {

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
    void shouldWriteFullBackupZipForCurrentAccount() throws Exception {
        Long userId = 7L;
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 4, 29, 12, 30, 0, 0, ZoneOffset.UTC);
        UserAccountEntity userAccount = createUserAccount();
        UserSettingEntity userSetting = createUserSetting(userId);
        WordSetEntity wordSet = createWordSet(userId);
        WordEntryEntity wordEntry = createWordEntry(wordSet.getId());
        AnkiTemplateEntity ankiTemplate = createAnkiTemplate(userId);
        MarkdownTemplateEntity markdownTemplate = createMarkdownTemplate(userId);
        NoteSourceEntity noteSource = createNoteSource(userId);
        StudyPlanEntity studyPlan = createStudyPlan(userId, wordSet.getId(), ankiTemplate.getId(), markdownTemplate.getId());
        CardInstanceEntity cardInstance = createCardInstance(studyPlan.getId(), wordEntry.getId());
        ReviewLogEntity reviewLog = createReviewLog(cardInstance.getId());
        NoteEntity note = createNote(userId, noteSource);
        NoteReviewLogEntity noteReviewLog = createNoteReviewLog(note.getId());

        when(userAccountRepository.findById(userId)).thenReturn(java.util.Optional.of(userAccount));
        when(userSettingRepository.findById(userId)).thenReturn(java.util.Optional.of(userSetting));
        when(wordSetRepository.findByOwnerUserIdOrderByIdAsc(userId)).thenReturn(List.of(wordSet));
        when(wordEntryRepository.findByWordSetIdInOrderByWordSetIdAscSourceOrderAscIdAsc(List.of(wordSet.getId()))).thenReturn(List.of(wordEntry));
        when(ankiTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId)).thenReturn(List.of(ankiTemplate));
        when(markdownTemplateRepository.findByOwnerUserIdOrderByIdAsc(userId)).thenReturn(List.of(markdownTemplate));
        when(noteSourceRepository.findByOwnerUserIdOrderByIdAsc(userId)).thenReturn(List.of(noteSource));
        when(studyPlanRepository.findByUserIdOrderByIdAsc(userId)).thenReturn(List.of(studyPlan));
        when(cardInstanceRepository.findByPlanIdInOrderByPlanIdAscSequenceNoAscStageNoAscIdAsc(List.of(studyPlan.getId()))).thenReturn(List.of(cardInstance));
        when(reviewLogRepository.findByCardInstanceIdInOrderByCardInstanceIdAscReviewedAtAscIdAsc(List.of(cardInstance.getId()))).thenReturn(List.of(reviewLog));
        when(noteRepository.findByUserIdOrderByIdAsc(userId)).thenReturn(List.of(note));
        when(noteReviewLogRepository.findByNoteIdInOrderByNoteIdAscReviewedAtAscIdAsc(List.of(note.getId()))).thenReturn(List.of(noteReviewLog));

        BackupPackageBuilder builder = new BackupPackageBuilder(
                objectMapper,
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        builder.writePackage(userId, BackupType.MANUAL_BACKUP, createdAt, outputStream);

        Map<String, JsonNode> entries = unzipJsonEntries(outputStream.toByteArray());

        assertTrue(entries.containsKey("manifest.json"));
        assertEquals("MANUAL_BACKUP", entries.get("manifest.json").get("backupType").asText());
        assertEquals(12, entries.get("manifest.json").get("files").size());
        assertEquals("Demo User", entries.get("data/user-profile.json").get("displayName").asText());
        assertFalse(entries.get("data/word-sets.json").get(0).has("ownerUserId"));
        assertEquals("plan-a", entries.get("data/study-plans.json").get(0).get("name").asText());
        assertEquals("card-json", entries.get("data/notes.json").get(0).get("fsrsCardJson").asText());
    }

    private Map<String, JsonNode> unzipJsonEntries(byte[] zipBytes) throws Exception {
        Map<String, JsonNode> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                zipInputStream.transferTo(outputStream);
                entries.put(entry.getName(), objectMapper.readTree(outputStream.toByteArray()));
                zipInputStream.closeEntry();
            }
        }
        return entries;
    }

    private UserAccountEntity createUserAccount() {
        UserAccountEntity entity = UserAccountEntity.create("Demo User");
        ReflectionTestUtils.setField(entity, "id", 7L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private UserSettingEntity createUserSetting(Long userId) {
        UserSettingEntity entity = UserSettingEntity.create(userId, "WORD_FIRST");
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private WordSetEntity createWordSet(Long userId) {
        WordSetEntity entity = WordSetEntity.createUserOwned("set-a", "desc", userId);
        ReflectionTestUtils.setField(entity, "id", 11L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private WordEntryEntity createWordEntry(Long wordSetId) {
        WordEntryEntity entity = WordEntryEntity.create(wordSetId, "勉強", "べんきょう", "study", "noun", "例文", "example", "N5", List.of("tag1"), 1);
        ReflectionTestUtils.setField(entity, "id", 12L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private AnkiTemplateEntity createAnkiTemplate(Long userId) {
        AnkiTemplateEntity entity = AnkiTemplateEntity.create(
                "anki-a",
                "desc",
                "USER",
                userId,
                Map.of("Front", List.of("expression")),
                "{{expression}}",
                "{{meaning}}",
                ".card{}"
        );
        ReflectionTestUtils.setField(entity, "id", 13L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private MarkdownTemplateEntity createMarkdownTemplate(Long userId) {
        MarkdownTemplateEntity entity = MarkdownTemplateEntity.create("md-a", "desc", "USER", userId, "# {{planName}}");
        ReflectionTestUtils.setField(entity, "id", 14L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private NoteSourceEntity createNoteSource(Long userId) {
        NoteSourceEntity entity = NoteSourceEntity.createUserOwned("note-a", "content", List.of("grammar"), userId);
        ReflectionTestUtils.setField(entity, "id", 15L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private StudyPlanEntity createStudyPlan(Long userId, Long wordSetId, Long ankiTemplateId, Long markdownTemplateId) {
        StudyPlanEntity entity = StudyPlanEntity.create("plan-a", userId, wordSetId, LocalDate.of(2026, 4, 29), 20, List.of(1, 3, 7), ankiTemplateId, markdownTemplateId);
        entity.activate();
        ReflectionTestUtils.setField(entity, "id", 16L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private CardInstanceEntity createCardInstance(Long planId, Long wordEntryId) {
        CardInstanceEntity entity = CardInstanceEntity.createFsrsCard(
                planId,
                wordEntryId,
                "NEW",
                1,
                0,
                OffsetDateTime.of(2026, 4, 29, 8, 0, 0, 0, ZoneOffset.UTC),
                "PENDING",
                "{\"state\":\"new\"}",
                0,
                null
        );
        ReflectionTestUtils.setField(entity, "id", 17L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private ReviewLogEntity createReviewLog(Long cardInstanceId) {
        ReviewLogEntity entity = ReviewLogEntity.create(cardInstanceId, OffsetDateTime.of(2026, 4, 29, 9, 0, 0, 0, ZoneOffset.UTC), "GOOD", 1200L, "ok");
        ReflectionTestUtils.setField(entity, "id", 18L);
        ReflectionTestUtils.setField(entity, "createdAt", OffsetDateTime.of(2026, 4, 29, 9, 0, 30, 0, ZoneOffset.UTC));
        return entity;
    }

    private NoteEntity createNote(Long userId, NoteSourceEntity noteSource) {
        NoteEntity entity = NoteEntity.create(noteSource, userId, OffsetDateTime.of(2026, 4, 29, 8, 0, 0, 0, ZoneOffset.UTC), "card-json");
        ReflectionTestUtils.setField(entity, "id", 19L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private NoteReviewLogEntity createNoteReviewLog(Long noteId) {
        NoteReviewLogEntity entity = NoteReviewLogEntity.create(noteId, OffsetDateTime.of(2026, 4, 29, 9, 30, 0, 0, ZoneOffset.UTC), "GOOD", 900L, "ok", "log-json");
        ReflectionTestUtils.setField(entity, "id", 20L);
        setAudit(entity, "2026-04-28T10:00:00Z", "2026-04-29T10:00:00Z");
        return entity;
    }

    private void setAudit(Object target, String createdAt, String updatedAt) {
        ReflectionTestUtils.setField(target, "createdAt", OffsetDateTime.parse(createdAt));
        ReflectionTestUtils.setField(target, "updatedAt", OffsetDateTime.parse(updatedAt));
    }
}
