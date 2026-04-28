package com.jp.vocab.card.service;

import com.jp.vocab.card.dto.ReviewCardRequest;
import com.jp.vocab.card.dto.ReviewCardResponse;
import com.jp.vocab.card.dto.ReviewLogResponse;
import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.entity.ReviewLogEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.card.repository.ReviewLogRepository;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CardReviewService {

    private static final Set<String> ALLOWED_RATINGS = Set.of("AGAIN", "HARD", "GOOD", "EASY");

    private final CardInstanceRepository cardInstanceRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final CardFsrsScheduler cardFsrsScheduler;
    private final CurrentUserService currentUserService;

    public CardReviewService(
            CardInstanceRepository cardInstanceRepository,
            ReviewLogRepository reviewLogRepository,
            CardFsrsScheduler cardFsrsScheduler,
            CurrentUserService currentUserService
    ) {
        this.cardInstanceRepository = cardInstanceRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.cardFsrsScheduler = cardFsrsScheduler;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ReviewCardResponse review(Long cardId, ReviewCardRequest request) {
        CardInstanceEntity card = getCard(cardId);
        String rating = normalizeRating(request.rating());
        String note = normalizeNote(request.note());
        OffsetDateTime reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String currentFsrsCardJson = card.getFsrsCardJson();
        int currentReviewCount = card.getFsrsCardJson() == null ? 0 : card.getReviewCount();
        if (currentFsrsCardJson == null || currentFsrsCardJson.isBlank()) {
            currentFsrsCardJson = cardFsrsScheduler.createInitialState().fsrsCardJson();
        }
        CardFsrsScheduler.ScheduledCardReview scheduled = cardFsrsScheduler.review(
                currentFsrsCardJson,
                rating,
                currentReviewCount,
                reviewedAt
        );

        ReviewLogEntity saved = reviewLogRepository.save(ReviewLogEntity.create(
                card.getId(),
                reviewedAt,
                rating,
                request.responseTimeMs(),
                note
        ));

        card.applyReviewResult(rating, request.sessionAgainCount(), reviewedAt);
        card.markDone(reviewedAt);
        cardInstanceRepository.save(card);
        cardInstanceRepository.save(card.createNextReviewCard(
                scheduled.fsrsCardJson(),
                scheduled.reviewCount(),
                scheduled.dueAt()
        ));

        return ReviewCardResponse.from(
                saved,
                card.getStatus(),
                card.isWeakFlag(),
                card.getWeakMarkedAt(),
                resolveTodayAction(rating, request.sessionAgainCount())
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewLogResponse> listReviews(Long cardId) {
        CardInstanceEntity card = getCard(cardId);
        return reviewLogRepository.findByCardInstanceIdOrderByReviewedAtDesc(card.getId())
                .stream()
                .map(ReviewLogResponse::from)
                .toList();
    }

    private CardInstanceEntity getCard(Long cardId) {
        return cardInstanceRepository.findOwnedById(cardId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Card not found: " + cardId));
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

        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveTodayAction(String rating, Integer sessionAgainCount) {
        if (!"AGAIN".equals(rating)) {
            return "DONE";
        }
        return sessionAgainCount != null && sessionAgainCount >= 2
                ? "MOVE_TO_WEAK_ROUND"
                : "REQUEUE_TODAY";
    }
}
