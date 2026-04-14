package com.jp.vocab.card.controller;

import com.jp.vocab.card.dto.CardCalendarItemResponse;
import com.jp.vocab.card.dto.TodayCardResponse;
import com.jp.vocab.card.service.CardQueryService;
import com.jp.vocab.shared.api.ApiResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/study-plans/{planId}/cards")
public class CardQueryController {

    private final CardQueryService cardQueryService;

    public CardQueryController(CardQueryService cardQueryService) {
        this.cardQueryService = cardQueryService;
    }

    @GetMapping("/today")
    public ApiResponse<List<TodayCardResponse>> getTodayCards(
            @PathVariable Long planId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(cardQueryService.getTodayCards(planId, date));
    }

    @GetMapping("/calendar")
    public ApiResponse<List<CardCalendarItemResponse>> getCalendar(
            @PathVariable Long planId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ApiResponse.success(cardQueryService.getCalendar(planId, start, end));
    }
}
