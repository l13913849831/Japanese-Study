package com.jp.vocab.card.service;

import com.jp.vocab.card.dto.ReviewCardRequest;
import com.jp.vocab.card.dto.ReviewCardResponse;
import com.jp.vocab.card.entity.CardInstanceEntity;
import com.jp.vocab.card.entity.ReviewLogEntity;
import com.jp.vocab.card.repository.CardInstanceRepository;
import com.jp.vocab.card.repository.ReviewLogRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardReviewServiceTest {

    @Mock
    private CardInstanceRepository cardInstanceRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    private final CardFsrsScheduler cardFsrsScheduler = new CardFsrsScheduler();

    private CardReviewService cardReviewService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId()).thenReturn(11L);
        cardReviewService = new CardReviewService(
                cardInstanceRepository,
                reviewLogRepository,
                cardFsrsScheduler,
                currentUserService
        );
    }

    @Test
    void shouldSaveReviewAndMarkCardDone() {
        CardInstanceEntity card = CardInstanceEntity.create(1L, 2L, "NEW", 1, 0, LocalDate.of(2026, 4, 24), "PENDING");
        ReflectionTestUtils.setField(card, "id", 7L);

        when(cardInstanceRepository.findOwnedById(7L, 11L)).thenReturn(Optional.of(card));
        when(reviewLogRepository.save(any(ReviewLogEntity.class))).thenAnswer(invocation -> {
            ReviewLogEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 88L);
            return saved;
        });
        when(cardInstanceRepository.save(card)).thenReturn(card);

        ReviewCardResponse response = cardReviewService.review(
                7L,
                new ReviewCardRequest(" good ", 2400L, 0, "  recalled after one second  ")
        );

        assertEquals(88L, response.reviewId());
        assertEquals(7L, response.cardId());
        assertEquals("GOOD", response.rating());
        assertEquals("DONE", response.cardStatus());
        assertEquals("DONE", response.todayAction());
        assertEquals(false, response.weak());
        assertEquals("DONE", card.getStatus());
        assertNotNull(card.getLastReviewedAt());

        ArgumentCaptor<ReviewLogEntity> logCaptor = ArgumentCaptor.forClass(ReviewLogEntity.class);
        verify(reviewLogRepository).save(logCaptor.capture());
        assertEquals("GOOD", logCaptor.getValue().getRating());
        assertEquals(2400L, logCaptor.getValue().getResponseTimeMs());
        assertEquals("recalled after one second", logCaptor.getValue().getNote());

        ArgumentCaptor<CardInstanceEntity> cardCaptor = ArgumentCaptor.forClass(CardInstanceEntity.class);
        verify(cardInstanceRepository, times(2)).save(cardCaptor.capture());
        CardInstanceEntity nextCard = cardCaptor.getAllValues().get(1);
        assertEquals("PENDING", nextCard.getStatus());
        assertEquals("REVIEW", nextCard.getCardType());
        assertEquals(1, nextCard.getReviewCount());
        assertNotNull(nextCard.getFsrsCardJson());
        assertNotNull(nextCard.getDueAt());
    }

    @Test
    void shouldRejectInvalidRatingBeforeSavingReview() {
        CardInstanceEntity card = CardInstanceEntity.create(1L, 2L, "NEW", 1, 0, LocalDate.of(2026, 4, 24), "PENDING");
        ReflectionTestUtils.setField(card, "id", 7L);
        when(cardInstanceRepository.findOwnedById(7L, 11L)).thenReturn(Optional.of(card));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cardReviewService.review(7L, new ReviewCardRequest("bad", 1000L, 0, "x"))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("rating is invalid", exception.getMessage());
        verify(reviewLogRepository, never()).save(any());
        verify(cardInstanceRepository, never()).save(any());
    }

    @Test
    void shouldMarkCardWeakAfterSecondAgainInSession() {
        CardInstanceEntity card = CardInstanceEntity.create(1L, 2L, "NEW", 1, 0, LocalDate.of(2026, 4, 24), "PENDING");
        ReflectionTestUtils.setField(card, "id", 7L);

        when(cardInstanceRepository.findOwnedById(7L, 11L)).thenReturn(Optional.of(card));
        when(reviewLogRepository.save(any(ReviewLogEntity.class))).thenAnswer(invocation -> {
            ReviewLogEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 89L);
            return saved;
        });
        when(cardInstanceRepository.save(card)).thenReturn(card);

        ReviewCardResponse response = cardReviewService.review(
                7L,
                new ReviewCardRequest("again", 800L, 2, "hard miss")
        );

        assertEquals(true, response.weak());
        assertEquals("MOVE_TO_WEAK_ROUND", response.todayAction());
        assertEquals("AGAIN", card.getLastReviewRating());
        assertEquals(true, card.isWeakFlag());

        ArgumentCaptor<CardInstanceEntity> cardCaptor = ArgumentCaptor.forClass(CardInstanceEntity.class);
        verify(cardInstanceRepository, times(2)).save(cardCaptor.capture());
        CardInstanceEntity nextCard = cardCaptor.getAllValues().get(1);
        assertEquals("PENDING", nextCard.getStatus());
        assertEquals(1, nextCard.getReviewCount());
        assertNotNull(nextCard.getDueAt());
    }
}
