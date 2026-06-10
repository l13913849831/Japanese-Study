package com.jp.vocab.learninglink.controller;

import com.jp.vocab.learninglink.dto.CreateLearningLinkRequest;
import com.jp.vocab.learninglink.dto.DeleteLearningLinkResponse;
import com.jp.vocab.learninglink.dto.LearningLinkResponse;
import com.jp.vocab.learninglink.service.LearningLinkService;
import com.jp.vocab.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning-links")
public class LearningLinkController {

    private final LearningLinkService learningLinkService;

    public LearningLinkController(LearningLinkService learningLinkService) {
        this.learningLinkService = learningLinkService;
    }

    @PostMapping
    public ApiResponse<LearningLinkResponse> create(@Valid @RequestBody CreateLearningLinkRequest request) {
        return ApiResponse.success(learningLinkService.create(request));
    }

    @GetMapping("/words/{wordEntryId}")
    public ApiResponse<List<LearningLinkResponse>> listByWordEntry(@PathVariable Long wordEntryId) {
        return ApiResponse.success(learningLinkService.listByWordEntry(wordEntryId));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<List<LearningLinkResponse>> listByNote(@PathVariable Long noteId) {
        return ApiResponse.success(learningLinkService.listByNote(noteId));
    }

    @DeleteMapping("/{linkId}")
    public ApiResponse<DeleteLearningLinkResponse> delete(@PathVariable Long linkId) {
        learningLinkService.delete(linkId);
        return ApiResponse.success(new DeleteLearningLinkResponse(true));
    }
}
