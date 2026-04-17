package com.jp.vocab.card.service;

import com.jp.vocab.card.dto.ReviewCardRequest;
import com.jp.vocab.card.dto.ReviewCardResponse;
import com.jp.vocab.card.dto.ReviewLogResponse;
import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.entity.ReviewLogEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.card.repository.ReviewLogRepository;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CardReviewService {

    private static final Set<String> ALLOWED_RATINGS = Set.of("AGAIN", "HARD", "GOOD", "EASY");

    private final CardInstanceRepository cardInstanceRepository;
    private final ReviewLogRepository reviewLogRepository;

    public CardReviewService(
            CardInstanceRepository cardInstanceRepository,
            ReviewLogRepository reviewLogRepository
    ) {
        this.cardInstanceRepository = cardInstanceRepository;
        this.reviewLogRepository = reviewLogRepository;
    }

    @Transactional
    public ReviewCardResponse review(Long cardId, ReviewCardRequest request) {
        CardInstanceEntity card = getCard(cardId);
        String rating = normalizeRating(request.rating());
        String note = normalizeNote(request.note());

        ReviewLogEntity saved = reviewLogRepository.save(ReviewLogEntity.create(
                card.getId(),
                OffsetDateTime.now(),
                rating,
                request.responseTimeMs(),
                note
        ));

        card.markDone();
        cardInstanceRepository.save(card);

        return ReviewCardResponse.from(saved, card.getStatus());
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
        return cardInstanceRepository.findById(cardId)
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
}
