package com.jp.vocab.weakitem.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.weakitem.dto.DismissWeakItemResponse;
import com.jp.vocab.weakitem.dto.WeakItemSummaryResponse;
import com.jp.vocab.weakitem.dto.WeakNoteItemResponse;
import com.jp.vocab.weakitem.dto.WeakWordItemResponse;
import com.jp.vocab.weakitem.service.WeakItemService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/weak-items")
public class WeakItemController {

    private final WeakItemService weakItemService;

    public WeakItemController(WeakItemService weakItemService) {
        this.weakItemService = weakItemService;
    }

    @GetMapping("/summary")
    public ApiResponse<WeakItemSummaryResponse> getSummary() {
        return ApiResponse.success(weakItemService.getSummary());
    }

    @GetMapping("/words")
    public ApiResponse<PageResponse<WeakWordItemResponse>> listWeakWords(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(weakItemService.listWeakWords(page, pageSize));
    }

    @GetMapping("/notes")
    public ApiResponse<PageResponse<WeakNoteItemResponse>> listWeakNotes(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(weakItemService.listWeakNotes(page, pageSize));
    }

    @PostMapping("/words/{cardId}/dismiss")
    public ApiResponse<DismissWeakItemResponse> dismissWeakWord(@PathVariable Long cardId) {
        weakItemService.dismissWeakWord(cardId);
        return ApiResponse.success(new DismissWeakItemResponse(true));
    }

    @PostMapping("/notes/{noteId}/dismiss")
    public ApiResponse<DismissWeakItemResponse> dismissWeakNote(@PathVariable Long noteId) {
        weakItemService.dismissWeakNote(noteId);
        return ApiResponse.success(new DismissWeakItemResponse(true));
    }
}
