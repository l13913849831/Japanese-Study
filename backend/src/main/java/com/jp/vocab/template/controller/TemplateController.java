package com.jp.vocab.template.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.template.dto.AnkiTemplateResponse;
import com.jp.vocab.template.dto.MarkdownTemplateResponse;
import com.jp.vocab.template.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
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
}
