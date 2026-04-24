package com.jp.vocab.note.dto;

import java.time.LocalDate;

public record NoteDashboardTrendItemResponse(
        LocalDate date,
        Integer reviewedNotes
) {
}
