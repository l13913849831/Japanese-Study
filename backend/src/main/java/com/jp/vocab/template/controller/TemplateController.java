package com.jp.vocab.template.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.template.dto.AnkiTemplatePreviewRequest;
import com.jp.vocab.template.dto.AnkiTemplatePreviewResponse;
import com.jp.vocab.template.dto.AnkiTemplateResponse;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewRequest;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewResponse;
import com.jp.vocab.template.dto.MarkdownTemplateResponse;
import com.jp.vocab.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/anki")
    public ApiResponse<List<AnkiTemplateResponse>> listAnkiTemplates() {
        return ApiResponse.success(templateService.listAnkiTemplates());
    }

    @GetMapping("/md")
    public ApiResponse<List<MarkdownTemplateResponse>> listMarkdownTemplates() {
        return ApiResponse.success(templateService.listMarkdownTemplates());
    }

    @PostMapping("/anki/preview")
    public ApiResponse<AnkiTemplatePreviewResponse> previewAnkiTemplate(
            @Valid @RequestBody AnkiTemplatePreviewRequest request
    ) {
        return ApiResponse.success(templateService.previewAnki(request));
    }

    @PostMapping("/md/preview")
    public ApiResponse<MarkdownTemplatePreviewResponse> previewMarkdownTemplate(
            @Valid @RequestBody MarkdownTemplatePreviewRequest request
    ) {
        return ApiResponse.success(templateService.previewMarkdown(request));
    }
}
