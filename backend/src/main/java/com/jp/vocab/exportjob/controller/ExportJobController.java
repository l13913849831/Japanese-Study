package com.jp.vocab.exportjob.controller;

import com.jp.vocab.exportjob.dto.ExportJobResponse;
import com.jp.vocab.exportjob.service.ExportJobService;
import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/export-jobs")
public class ExportJobController {

    private final ExportJobService exportJobService;

    public ExportJobController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ExportJobResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(exportJobService.list(page, pageSize));
    }
}
