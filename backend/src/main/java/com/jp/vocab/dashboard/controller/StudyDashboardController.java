package com.jp.vocab.dashboard.controller;

import com.jp.vocab.dashboard.dto.StudyDashboardResponse;
import com.jp.vocab.dashboard.service.StudyDashboardService;
import com.jp.vocab.shared.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class StudyDashboardController {

    private final StudyDashboardService studyDashboardService;

    public StudyDashboardController(StudyDashboardService studyDashboardService) {
        this.studyDashboardService = studyDashboardService;
    }

    @GetMapping
    public ApiResponse<StudyDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return ApiResponse.success(studyDashboardService.getDashboard(targetDate));
    }
}
