package com.jp.vocab.wordset.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.wordset.dto.CreateWordSetRequest;
import com.jp.vocab.wordset.dto.SaveWordEntryRequest;
import com.jp.vocab.wordset.dto.WordEntryImportResponse;
import com.jp.vocab.wordset.dto.WordEntryResponse;
import com.jp.vocab.wordset.dto.WordSetResponse;
import com.jp.vocab.wordset.service.WordEntryService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/word-sets")
public class WordSetController {

    private final WordSetService wordSetService;
    private final WordEntryService wordEntryService;

    public WordSetController(WordSetService wordSetService, WordEntryService wordEntryService) {
        this.wordSetService = wordSetService;
        this.wordEntryService = wordEntryService;
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

    @GetMapping("/{wordSetId}/words")
    public ApiResponse<PageResponse<WordEntryResponse>> listWordEntries(
            @PathVariable Long wordSetId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String tag
    ) {
        return ApiResponse.success(wordEntryService.list(wordSetId, page, pageSize, keyword, level, tag));
    }

    @PostMapping("/{wordSetId}/words")
    public ApiResponse<WordEntryResponse> createWordEntry(
            @PathVariable Long wordSetId,
            @Valid @RequestBody SaveWordEntryRequest request
    ) {
        return ApiResponse.success(wordEntryService.create(wordSetId, request));
    }

    @PostMapping(path = "/{wordSetId}/import", consumes = "multipart/form-data")
    public ApiResponse<WordEntryImportResponse> importWordEntries(
            @PathVariable Long wordSetId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(wordEntryService.importEntries(wordSetId, file));
    }
}
