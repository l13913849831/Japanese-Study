package com.jp.vocab.studyplan.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.studyplan.dto.StudyPlanResponse;
import com.jp.vocab.studyplan.service.StudyPlanService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/study-plans")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudyPlanResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(studyPlanService.list(page, pageSize));
    }
}
