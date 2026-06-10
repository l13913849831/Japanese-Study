package com.jp.vocab.exportjob.dto;

import java.time.LocalDate;

public record ExportJobPreflightResponse(
        Long planId,
        String planName,
        String exportType,
        LocalDate targetDate,
        int totalCards,
        int newCards,
        int reviewCards,
        String markdownTemplateName,
        boolean creatable,
        String message
) {
}
