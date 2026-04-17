package com.jp.vocab.wordset.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.wordset.dto.DeleteWordEntryResponse;
import com.jp.vocab.wordset.dto.SaveWordEntryRequest;
import com.jp.vocab.wordset.dto.WordEntryResponse;
import com.jp.vocab.wordset.service.WordEntryService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/words")
public class WordEntryController {

    private final WordEntryService wordEntryService;

    public WordEntryController(WordEntryService wordEntryService) {
        this.wordEntryService = wordEntryService;
    }

    @PutMapping("/{wordId}")
    public ApiResponse<WordEntryResponse> update(
            @PathVariable Long wordId,
            @Valid @RequestBody SaveWordEntryRequest request
    ) {
        return ApiResponse.success(wordEntryService.update(wordId, request));
    }

    @DeleteMapping("/{wordId}")
    public ApiResponse<DeleteWordEntryResponse> delete(@PathVariable Long wordId) {
        return ApiResponse.success(wordEntryService.delete(wordId));
    }
}
