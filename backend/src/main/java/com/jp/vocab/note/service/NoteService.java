package com.jp.vocab.note.service;

import com.jp.vocab.note.dto.ImportNotesRequest;
import com.jp.vocab.note.dto.ImportNotesRequestItem;
import com.jp.vocab.note.dto.NoteImportErrorResponse;
import com.jp.vocab.note.dto.NoteImportPreviewItemResponse;
import com.jp.vocab.note.dto.NoteImportPreviewResponse;
import com.jp.vocab.note.dto.NoteImportResponse;
import com.jp.vocab.note.dto.NoteResponse;
import com.jp.vocab.note.dto.SaveNoteRequest;
import com.jp.vocab.note.entity.NoteEntity;
import com.jp.vocab.note.repository.NoteRepository;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteMarkdownParser noteMarkdownParser;
    private final NoteFsrsScheduler noteFsrsScheduler;

    public NoteService(
            NoteRepository noteRepository,
            NoteMarkdownParser noteMarkdownParser,
            NoteFsrsScheduler noteFsrsScheduler
    ) {
        this.noteRepository = noteRepository;
        this.noteMarkdownParser = noteMarkdownParser;
        this.noteFsrsScheduler = noteFsrsScheduler;
    }

    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> list(
            int page,
            int pageSize,
            String keyword,
            String tag,
            String masteryStatus
    ) {
        List<NoteResponse> items = noteRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt", "id"))
                .stream()
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesTag(item, tag))
                .filter(item -> matchesMasteryStatus(item, masteryStatus))
                .map(NoteResponse::from)
                .toList();

        if (items.isEmpty()) {
            return PageResponse.empty(page, pageSize);
        }

        int normalizedPage = Math.max(page, 1);
        int startIndex = Math.min((normalizedPage - 1) * pageSize, items.size());
        int endIndex = Math.min(startIndex + pageSize, items.size());
        return new PageResponse<>(
                items.subList(startIndex, endIndex),
                normalizedPage,
                pageSize,
                items.size()
        );
    }

    @Transactional
    public NoteResponse create(SaveNoteRequest request) {
        SanitizedNote sanitized = sanitize(request.title(), request.content(), request.tags());
        NoteFsrsScheduler.ScheduledNoteReview initialState = noteFsrsScheduler.createInitialState();
        NoteEntity saved = noteRepository.save(NoteEntity.create(
                sanitized.title(),
                sanitized.content(),
                sanitized.tags(),
                initialState.dueAt(),
                initialState.fsrsCardJson()
        ));
        return NoteResponse.from(saved);
    }

    @Transactional
    public NoteResponse update(Long noteId, SaveNoteRequest request) {
        NoteEntity entity = getEntity(noteId);
        SanitizedNote sanitized = sanitize(request.title(), request.content(), request.tags());
        entity.update(
                sanitized.title(),
                sanitized.content(),
                sanitized.tags()
        );
        return NoteResponse.from(noteRepository.save(entity));
    }

    @Transactional
    public void delete(Long noteId) {
        noteRepository.delete(getEntity(noteId));
    }

    @Transactional(readOnly = true)
    public NoteImportPreviewResponse previewImport(
            MultipartFile file,
            String splitMode,
            String commonTagsText
    ) {
        validateUpload(file);
        NoteMarkdownParser.ParsedMarkdownNotes parsed = noteMarkdownParser.parse(
                readMarkdown(file),
                splitMode,
                parseTags(commonTagsText)
        );
        return new NoteImportPreviewResponse(
                parsed.splitMode(),
                parsed.items().size(),
                parsed.readyCount(),
                parsed.errorCount(),
                parsed.items().stream()
                        .map(item -> new NoteImportPreviewItemResponse(
                                item.itemId(),
                                item.title(),
                                item.content(),
                                item.tags(),
                                item.status(),
                                item.message()
                        ))
                        .toList()
        );
    }

    @Transactional
    public NoteImportResponse importNotes(ImportNotesRequest request) {
        int importedCount = 0;
        List<NoteImportErrorResponse> errors = new ArrayList<>();

        for (int index = 0; index < request.items().size(); index++) {
            ImportNotesRequestItem item = request.items().get(index);
            try {
                SanitizedNote sanitized = sanitize(item.title(), item.content(), item.tags());
                NoteFsrsScheduler.ScheduledNoteReview initialState = noteFsrsScheduler.createInitialState();
                noteRepository.save(NoteEntity.create(
                        sanitized.title(),
                        sanitized.content(),
                        sanitized.tags(),
                        initialState.dueAt(),
                        initialState.fsrsCardJson()
                ));
                importedCount++;
            } catch (BusinessException ex) {
                errors.add(new NoteImportErrorResponse(index, "item", ex.getMessage()));
            }
        }

        return new NoteImportResponse(
                importedCount,
                request.items().size() - importedCount,
                errors
        );
    }

    NoteEntity getEntity(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Note not found: " + noteId));
    }

    private String readMarkdown(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to read Markdown file");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Markdown file must not be empty");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Only .md files are supported");
        }
    }

    private SanitizedNote sanitize(String title, String content, List<String> tags) {
        String normalizedTitle = normalizeRequired(title, "title must not be blank", 255);
        String normalizedContent = normalizeRequired(content, "content must not be blank", null);
        return new SanitizedNote(
                normalizedTitle,
                normalizedContent,
                normalizeTags(tags)
        );
    }

    private String normalizeRequired(String value, String errorMessage, Integer maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, errorMessage);
        }
        if (maxLength != null && normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title must be at most 255 characters");
        }
        return normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = tag == null ? "" : tag.trim();
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private List<String> parseTags(String commonTagsText) {
        if (commonTagsText == null || commonTagsText.isBlank()) {
            return List.of();
        }
        return normalizeTags(List.of(commonTagsText.split(",")));
    }

    private boolean matchesKeyword(NoteEntity entity, String keyword) {
        String normalized = normalizeFilter(keyword);
        return normalized == null
                || containsIgnoreCase(entity.getTitle(), normalized)
                || containsIgnoreCase(entity.getContent(), normalized)
                || entity.getTags().stream().anyMatch(tag -> containsIgnoreCase(tag, normalized));
    }

    private boolean matchesTag(NoteEntity entity, String tag) {
        String normalized = normalizeFilter(tag);
        return normalized == null
                || entity.getTags().stream().anyMatch(item -> item.equalsIgnoreCase(normalized));
    }

    private boolean matchesMasteryStatus(NoteEntity entity, String masteryStatus) {
        String normalized = normalizeFilter(masteryStatus);
        return normalized == null || entity.getMasteryStatus().equalsIgnoreCase(normalized);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private record SanitizedNote(String title, String content, List<String> tags) {
    }
}
