package com.jp.vocab.dashboard.controller;

import com.jp.vocab.dashboard.dto.LongTermDashboardResponse;
import com.jp.vocab.dashboard.dto.StudyDashboardResponse;
import com.jp.vocab.dashboard.service.LongTermDashboardService;
import com.jp.vocab.dashboard.service.StudyDashboardService;
import com.jp.vocab.shared.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Validated
public class StudyDashboardController {

    private final StudyDashboardService studyDashboardService;
    private final LongTermDashboardService longTermDashboardService;

    public StudyDashboardController(
            StudyDashboardService studyDashboardService,
            LongTermDashboardService longTermDashboardService
    ) {
        this.studyDashboardService = studyDashboardService;
        this.longTermDashboardService = longTermDashboardService;
    }

    @GetMapping
    public ApiResponse<StudyDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return ApiResponse.success(studyDashboardService.getDashboard(targetDate));
    }

    @GetMapping("/long-term")
    public ApiResponse<LongTermDashboardResponse> getLongTermDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "90") @Min(30) @Max(180) int rangeDays
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return ApiResponse.success(longTermDashboardService.getDashboard(targetDate, rangeDays));
    }
}
