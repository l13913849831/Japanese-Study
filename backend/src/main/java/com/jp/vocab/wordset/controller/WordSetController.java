package com.jp.vocab.wordset.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.wordset.dto.CreateWordSetRequest;
import com.jp.vocab.wordset.dto.WordSetResponse;
import com.jp.vocab.wordset.service.WordSetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/word-sets")
public class WordSetController {

    private final WordSetService wordSetService;

    public WordSetController(WordSetService wordSetService) {
        this.wordSetService = wordSetService;
    }

    @PostMapping
    public ApiResponse<WordSetResponse> create(@Valid @RequestBody CreateWordSetRequest request) {
        return ApiResponse.success(wordSetService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<WordSetResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(wordSetService.list(page, pageSize));
    }
}
