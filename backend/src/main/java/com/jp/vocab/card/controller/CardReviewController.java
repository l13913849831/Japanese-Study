package com.jp.vocab.card.controller;

import com.jp.vocab.card.dto.ReviewCardRequest;
import com.jp.vocab.card.dto.ReviewCardResponse;
import com.jp.vocab.card.dto.ReviewLogResponse;
import com.jp.vocab.card.service.CardReviewService;
import com.jp.vocab.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardReviewController {

    private final CardReviewService cardReviewService;

    public CardReviewController(CardReviewService cardReviewService) {
        this.cardReviewService = cardReviewService;
    }

    @PostMapping("/{cardId}/review")
    public ApiResponse<ReviewCardResponse> review(
            @PathVariable Long cardId,
            @Valid @RequestBody ReviewCardRequest request
    ) {
        return ApiResponse.success(cardReviewService.review(cardId, request));
    }

    @GetMapping("/{cardId}/reviews")
    public ApiResponse<List<ReviewLogResponse>> listReviews(@PathVariable Long cardId) {
        return ApiResponse.success(cardReviewService.listReviews(cardId));
    }
}
