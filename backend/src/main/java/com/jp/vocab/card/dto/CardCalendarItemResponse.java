package com.jp.vocab.card.dto;

public record CardCalendarItemResponse(
        String date,
        int newCards,
        int reviewCards
) {
}
