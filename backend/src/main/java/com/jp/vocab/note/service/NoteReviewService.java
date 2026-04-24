package com.jp.vocab.note.service;

import com.jp.vocab.note.dto.NoteReviewLogResponse;
import com.jp.vocab.note.dto.NoteReviewQueueItemResponse;
import com.jp.vocab.note.dto.ReviewNoteRequest;
import com.jp.vocab.note.dto.ReviewNoteResponse;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.entity.NoteReviewLogEntity;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.note.repository.NoteReviewLogRepository;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NoteReviewService {

    private static final Set<String> ALLOWED_RATINGS = Set.of("AGAIN", "HARD", "GOOD", "EASY");

    private final NoteRepository noteRepository;
    private final NoteReviewLogRepository noteReviewLogRepository;
    private final NoteFsrsScheduler noteFsrsScheduler;

    public NoteReviewService(
            NoteRepository noteRepository,
            NoteReviewLogRepository noteReviewLogRepository,
            NoteFsrsScheduler noteFsrsScheduler
    ) {
        this.noteRepository = noteRepository;
        this.noteReviewLogRepository = noteReviewLogRepository;
        this.noteFsrsScheduler = noteFsrsScheduler;
    }

    @Transactional(readOnly = true)
    public List<NoteReviewQueueItemResponse> listDueNotes(LocalDate targetDate) {
        OffsetDateTime endExclusive = targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return noteRepository.findAll()
                .stream()
                .filter(item -> item.getDueAt().isBefore(endExclusive))
                .sorted((left, right) -> {
                    int dueCompare = left.getDueAt().compareTo(right.getDueAt());
                    if (dueCompare != 0) {
                        return dueCompare;
                    }
                    return left.getId().compareTo(right.getId());
                })
                .map(NoteReviewQueueItemResponse::from)
                .toList();
    }

    @Transactional
    public ReviewNoteResponse review(Long noteId, ReviewNoteRequest request) {
        NoteEntity entity = getEntity(noteId);
        String rating = normalizeRating(request.rating());
        String note = normalizeNote(request.note());
        OffsetDateTime reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
        NoteFsrsScheduler.ScheduledNoteReview scheduled = noteFsrsScheduler.review(
                entity.getFsrsCardJson(),
                rating,
                entity.getReviewCount(),
                reviewedAt
        );

        entity.applyReview(
                scheduled.reviewCount(),
                scheduled.masteryStatus(),
                scheduled.dueAt(),
                scheduled.reviewedAt(),
                scheduled.fsrsCardJson()
        );

        NoteReviewLogEntity saved = noteReviewLogRepository.save(NoteReviewLogEntity.create(
                entity.getId(),
                reviewedAt,
                rating,
                request.responseTimeMs(),
                note,
                scheduled.fsrsReviewLogJson()
        ));
        noteRepository.save(entity);
        return ReviewNoteResponse.from(saved, entity.getMasteryStatus(), entity.getDueAt());
    }

    @Transactional(readOnly = true)
    public List<NoteReviewLogResponse> listReviews(Long noteId) {
        getEntity(noteId);
        return noteReviewLogRepository.findByNoteIdOrderByReviewedAtDescIdDesc(noteId)
                .stream()
                .map(NoteReviewLogResponse::from)
                .toList();
    }

    private NoteEntity getEntity(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Note not found: " + noteId));
    }

    private String normalizeRating(String rating) {
        String normalized = rating == null ? "" : rating.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_RATINGS.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "rating is invalid");
        }
        return normalized;
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
