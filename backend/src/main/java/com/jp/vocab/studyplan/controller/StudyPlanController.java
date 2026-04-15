package com.jp.vocab.studyplan.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.studyplan.dto.CreateStudyPlanRequest;
import com.jp.vocab.studyplan.dto.StudyPlanResponse;
import com.jp.vocab.studyplan.dto.UpdateStudyPlanRequest;
import com.jp.vocab.studyplan.service.StudyPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/{id}")
    public ApiResponse<StudyPlanResponse> get(@PathVariable Long id) {
        return ApiResponse.success(studyPlanService.get(id));
    }

    @PostMapping
    public ApiResponse<StudyPlanResponse> create(@Valid @RequestBody CreateStudyPlanRequest request) {
        return ApiResponse.success(studyPlanService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyPlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudyPlanRequest request
    ) {
        return ApiResponse.success(studyPlanService.update(id, request));
    }
}
