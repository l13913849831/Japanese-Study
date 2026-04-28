package com.jp.vocab.note.service;

import com.jp.vocab.note.dto.NoteReviewQueueItemResponse;
import com.jp.vocab.note.dto.ReviewNoteRequest;
import com.jp.vocab.note.dto.ReviewNoteResponse;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.entity.NoteSourceEntity;
import com.jp.vocab.note.entity.NoteReviewLogEntity;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.note.repository.NoteReviewLogRepository;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteReviewServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteReviewLogRepository noteReviewLogRepository;

    @Mock
    private NoteFsrsScheduler noteFsrsScheduler;

    @Mock
    private CurrentUserService currentUserService;

    private NoteReviewService noteReviewService;

    @BeforeEach
    void setUp() {
        noteReviewService = new NoteReviewService(
                noteRepository,
                noteReviewLogRepository,
                noteFsrsScheduler,
                currentUserService
        );
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void shouldListDueNotesSortedByDueAtThenId() {
        LocalDate targetDate = LocalDate.of(2026, 4, 24);
        NoteEntity first = createNoteEntity(1L, "First", targetDate.atTime(8, 0).atOffset(ZoneOffset.UTC));
        NoteEntity second = createNoteEntity(2L, "Second", targetDate.atTime(10, 0).atOffset(ZoneOffset.UTC));
        NoteEntity excluded = createNoteEntity(3L, "Excluded", targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        when(noteRepository.findByUserIdAndDueAtBeforeOrderByDueAtAscIdAsc(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(List.of(first, second));

        List<NoteReviewQueueItemResponse> response = noteReviewService.listDueNotes(targetDate);

        assertEquals(2, response.size());
        assertIterableEquals(List.of(1L, 2L), response.stream().map(NoteReviewQueueItemResponse::id).toList());
    }

    @Test
    void shouldPersistReviewAndUpdateNoteState() {
        OffsetDateTime reviewedAt = OffsetDateTime.of(2026, 4, 24, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime nextDueAt = reviewedAt.plusDays(3);
        NoteEntity entity = createNoteEntity(10L, "Java", reviewedAt.minusDays(1));

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));
        when(noteFsrsScheduler.review(eq(entity.getFsrsCardJson()), eq("GOOD"), eq(0), any(OffsetDateTime.class)))
                .thenReturn(new NoteFsrsScheduler.ScheduledNoteReview(
                        "next-card-json",
                        nextDueAt,
                        "LEARNING",
                        1,
                        reviewedAt,
                        "review-log-json"
                ));
        when(noteReviewLogRepository.save(any(NoteReviewLogEntity.class))).thenAnswer(invocation -> {
            NoteReviewLogEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });
        when(noteRepository.save(entity)).thenReturn(entity);

        ReviewNoteResponse response = noteReviewService.review(
                10L,
                new ReviewNoteRequest(" good ", 1800L, 0, "  remembered after hint  ")
        );

        assertEquals(99L, response.reviewId());
        assertEquals(10L, response.noteId());
        assertEquals("GOOD", response.rating());
        assertEquals("LEARNING", response.masteryStatus());
        assertEquals("DONE", response.todayAction());
        assertEquals(false, response.weak());
        assertEquals(nextDueAt, response.dueAt());
        assertEquals(1, entity.getReviewCount());
        assertEquals("LEARNING", entity.getMasteryStatus());
        assertEquals(nextDueAt, entity.getDueAt());
        assertEquals("next-card-json", entity.getFsrsCardJson());

        ArgumentCaptor<NoteReviewLogEntity> logCaptor = ArgumentCaptor.forClass(NoteReviewLogEntity.class);
        verify(noteReviewLogRepository).save(logCaptor.capture());
        assertEquals("GOOD", logCaptor.getValue().getRating());
        assertEquals(1800L, logCaptor.getValue().getResponseTimeMs());
        assertEquals("remembered after hint", logCaptor.getValue().getNoteText());
    }

    @Test
    void shouldRejectInvalidRatingBeforePersistingReview() {
        NoteEntity entity = createNoteEntity(10L, "Java", OffsetDateTime.now(ZoneOffset.UTC));
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> noteReviewService.review(10L, new ReviewNoteRequest("invalid", 800L, 0, "bad"))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("rating is invalid", exception.getMessage());
        verify(noteFsrsScheduler, never()).review(any(), any(), any(), any());
        verify(noteReviewLogRepository, never()).save(any());
    }

    @Test
    void shouldMarkNoteWeakAfterSecondAgainInSession() {
        OffsetDateTime reviewedAt = OffsetDateTime.of(2026, 4, 24, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime nextDueAt = reviewedAt.plusHours(4);
        NoteEntity entity = createNoteEntity(10L, "Java", reviewedAt.minusDays(1));

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));
        when(noteFsrsScheduler.review(eq(entity.getFsrsCardJson()), eq("AGAIN"), eq(0), any(OffsetDateTime.class)))
                .thenReturn(new NoteFsrsScheduler.ScheduledNoteReview(
                        "next-card-json",
                        nextDueAt,
                        "LEARNING",
                        1,
                        reviewedAt,
                        "review-log-json"
                ));
        when(noteReviewLogRepository.save(any(NoteReviewLogEntity.class))).thenAnswer(invocation -> {
            NoteReviewLogEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(noteRepository.save(entity)).thenReturn(entity);

        ReviewNoteResponse response = noteReviewService.review(
                10L,
                new ReviewNoteRequest("again", 1200L, 2, "forgot")
        );

        assertEquals(true, response.weak());
        assertEquals("MOVE_TO_WEAK_ROUND", response.todayAction());
        assertEquals(true, entity.isWeakFlag());
        assertEquals("AGAIN", entity.getLastReviewRating());
    }

    private NoteEntity createNoteEntity(Long id, String title, OffsetDateTime dueAt) {
        NoteSourceEntity source = NoteSourceEntity.createUserOwned(title, title + " content", List.of("tag"), 1L);
        ReflectionTestUtils.setField(source, "id", id);
        NoteEntity entity = NoteEntity.create(source, 1L, dueAt, "card-json");
        ReflectionTestUtils.setField(entity, "id", id);
        assertNull(entity.getLastReviewedAt());
        return entity;
    }
}
