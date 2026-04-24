package com.jp.vocab.note.controller;

import com.jp.vocab.note.dto.DeleteNoteResponse;
import com.jp.vocab.note.dto.ImportNotesRequest;
import com.jp.vocab.note.dto.NoteDashboardResponse;
import com.jp.vocab.note.dto.NoteImportPreviewResponse;
import com.jp.vocab.note.dto.NoteImportResponse;
import com.jp.vocab.note.dto.NoteResponse;
import com.jp.vocab.note.dto.NoteReviewLogResponse;
import com.jp.vocab.note.dto.NoteReviewQueueItemResponse;
import com.jp.vocab.note.dto.ReviewNoteRequest;
import com.jp.vocab.note.dto.ReviewNoteResponse;
import com.jp.vocab.note.dto.SaveNoteRequest;
import com.jp.vocab.note.service.NoteDashboardService;
import com.jp.vocab.note.service.NoteReviewService;
import com.jp.vocab.note.service.NoteService;
import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final NoteReviewService noteReviewService;
    private final NoteDashboardService noteDashboardService;

    public NoteController(
            NoteService noteService,
            NoteReviewService noteReviewService,
            NoteDashboardService noteDashboardService
    ) {
        this.noteService = noteService;
        this.noteReviewService = noteReviewService;
        this.noteDashboardService = noteDashboardService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NoteResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String masteryStatus
    ) {
        return ApiResponse.success(noteService.list(page, pageSize, keyword, tag, masteryStatus));
    }

    @PostMapping
    public ApiResponse<NoteResponse> create(@Valid @RequestBody SaveNoteRequest request) {
        return ApiResponse.success(noteService.create(request));
    }

    @PutMapping("/{noteId}")
    public ApiResponse<NoteResponse> update(
            @PathVariable Long noteId,
            @Valid @RequestBody SaveNoteRequest request
    ) {
        return ApiResponse.success(noteService.update(noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<DeleteNoteResponse> delete(@PathVariable Long noteId) {
        noteService.delete(noteId);
        return ApiResponse.success(new DeleteNoteResponse(true));
    }

    @PostMapping(path = "/import/preview", consumes = "multipart/form-data")
    public ApiResponse<NoteImportPreviewResponse> previewImport(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "splitMode", required = false) String splitMode,
            @RequestPart(value = "commonTagsText", required = false) String commonTagsText
    ) {
        return ApiResponse.success(noteService.previewImport(file, splitMode, commonTagsText));
    }

    @PostMapping("/import")
    public ApiResponse<NoteImportResponse> importNotes(@Valid @RequestBody ImportNotesRequest request) {
        return ApiResponse.success(noteService.importNotes(request));
    }

    @GetMapping("/reviews/today")
    public ApiResponse<List<NoteReviewQueueItemResponse>> listDueNotes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(noteReviewService.listDueNotes(date == null ? LocalDate.now() : date));
    }

    @PostMapping("/{noteId}/reviews")
    public ApiResponse<ReviewNoteResponse> review(
            @PathVariable Long noteId,
            @Valid @RequestBody ReviewNoteRequest request
    ) {
        return ApiResponse.success(noteReviewService.review(noteId, request));
    }

    @GetMapping("/{noteId}/reviews")
    public ApiResponse<List<NoteReviewLogResponse>> listReviews(@PathVariable Long noteId) {
        return ApiResponse.success(noteReviewService.listReviews(noteId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<NoteDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(noteDashboardService.getDashboard(date == null ? LocalDate.now() : date));
    }
}
