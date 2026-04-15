package com.jp.vocab.exportjob.controller;

import com.jp.vocab.exportjob.dto.CreateExportJobRequest;
import com.jp.vocab.exportjob.dto.ExportJobResponse;
import com.jp.vocab.exportjob.entity.ExportJobEntity;
import com.jp.vocab.exportjob.service.ExportJobService;
import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ApiResponse<ExportJobResponse> create(@Valid @RequestBody CreateExportJobRequest request) {
        return ApiResponse.success(exportJobService.create(request));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ExportJobEntity exportJob = exportJobService.getEntity(id);
        Resource resource = exportJobService.getDownloadResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportJob.getFileName() + "\"")
                .contentType(resolveContentType(exportJob.getExportType()))
                .body(resource);
    }

    private MediaType resolveContentType(String exportType) {
        return switch (exportType) {
            case "ANKI_CSV" -> MediaType.valueOf("text/csv");
            case "ANKI_TSV" -> MediaType.valueOf("text/tab-separated-values");
            case "MARKDOWN" -> MediaType.valueOf("text/markdown");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
