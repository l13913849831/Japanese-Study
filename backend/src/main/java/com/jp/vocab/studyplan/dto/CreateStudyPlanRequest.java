package com.jp.vocab.studyplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateStudyPlanRequest(
        @NotBlank(message = "name must not be blank")
        String name,
        @NotNull(message = "wordSetId must not be null")
        Long wordSetId,
        @NotNull(message = "startDate must not be null")
        LocalDate startDate,
        @NotNull(message = "dailyNewCount must not be null")
        @Min(value = 1, message = "dailyNewCount must be greater than 0")
        Integer dailyNewCount,
        @NotEmpty(message = "reviewOffsets must not be empty")
        List<Integer> reviewOffsets,
        Long ankiTemplateId,
        Long mdTemplateId,
        @NotBlank(message = "status must not be blank")
        String status
) {
}
