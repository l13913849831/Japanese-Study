package com.jp.vocab.note.dto;

import java.time.LocalDate;

public record NoteDashboardOverviewResponse(
        LocalDate date,
        Integer dueToday,
        Integer totalNotes,
        Integer reviewedNotes
) {
}
