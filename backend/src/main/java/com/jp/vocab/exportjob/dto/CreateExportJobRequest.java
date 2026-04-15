package com.jp.vocab.exportjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateExportJobRequest(
        @NotNull(message = "planId must not be null")
        Long planId,
        @NotBlank(message = "exportType must not be blank")
        String exportType,
        @NotNull(message = "targetDate must not be null")
        LocalDate targetDate
) {
}
