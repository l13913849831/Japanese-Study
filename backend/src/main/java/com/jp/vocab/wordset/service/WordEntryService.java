package com.jp.vocab.wordset.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.csv.CsvRow;
import com.jp.vocab.shared.csv.SimpleCsvParser;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.wordset.dto.WordEntryImportError;
import com.jp.vocab.wordset.dto.WordEntryImportResponse;
import com.jp.vocab.wordset.dto.WordEntryResponse;
import com.jp.vocab.wordset.entity.WordEntryEntity;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WordEntryService {

    private static final List<String> REQUIRED_HEADERS = List.of("expression", "meaning");

    private final WordEntryRepository wordEntryRepository;
    private final WordSetRepository wordSetRepository;
    private final SimpleCsvParser csvParser;

    public WordEntryService(
            WordEntryRepository wordEntryRepository,
            WordSetRepository wordSetRepository,
            SimpleCsvParser csvParser
    ) {
        this.wordEntryRepository = wordEntryRepository;
        this.wordSetRepository = wordSetRepository;
        this.csvParser = csvParser;
    }

    @Transactional(readOnly = true)
    public PageResponse<WordEntryResponse> list(Long wordSetId, int page, int pageSize) {
        ensureWordSetExists(wordSetId);
        return PageResponse.from(
                wordEntryRepository.findByWordSetId(
                                wordSetId,
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "sourceOrder")))
                        .map(WordEntryResponse::from)
        );
    }

    @Transactional
    public WordEntryImportResponse importCsv(Long wordSetId, MultipartFile file) {
        ensureWordSetExists(wordSetId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "CSV file must not be empty");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to read CSV file");
        }

        List<CsvRow> rows = csvParser.parse(content);
        if (rows.isEmpty()) {
            return new WordEntryImportResponse(0, 0, List.of());
        }

        validateHeaders(rows.getFirst().values().keySet().stream().toList());

        List<WordEntryEntity> existingEntries = wordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(wordSetId);
        Set<String> knownKeys = new HashSet<>();
        int maxSourceOrder = 0;
        for (WordEntryEntity existingEntry : existingEntries) {
            knownKeys.add(buildDedupKey(existingEntry.getExpression(), existingEntry.getReading()));
            maxSourceOrder = Math.max(maxSourceOrder, existingEntry.getSourceOrder());
        }

        List<WordEntryEntity> toSave = new ArrayList<>();
        List<WordEntryImportError> errors = new ArrayList<>();
        int skippedCount = 0;
        int nextSourceOrder = maxSourceOrder + 1;

        for (CsvRow row : rows) {
            String expression = normalize(row.values().get("expression"));
            String meaning = normalize(row.values().get("meaning"));

            if (expression.isBlank()) {
                errors.add(new WordEntryImportError(row.lineNumber(), "expression", "expression must not be blank"));
                continue;
            }
            if (meaning.isBlank()) {
                errors.add(new WordEntryImportError(row.lineNumber(), "meaning", "meaning must not be blank"));
                continue;
            }

            String reading = normalize(row.values().get("reading"));
            String key = buildDedupKey(expression, reading);
            if (knownKeys.contains(key)) {
                skippedCount++;
                continue;
            }

            knownKeys.add(key);
            toSave.add(WordEntryEntity.create(
                    wordSetId,
                    expression,
                    reading.isBlank() ? null : reading,
                    meaning,
                    nullable(row.values().get("partOfSpeech")),
                    nullable(row.values().get("exampleJp")),
                    nullable(row.values().get("exampleZh")),
                    nullable(row.values().get("level")),
                    parseTags(row.values().get("tags")),
                    nextSourceOrder++
            ));
        }

        wordEntryRepository.saveAll(toSave);
        return new WordEntryImportResponse(toSave.size(), skippedCount, errors);
    }

    private void ensureWordSetExists(Long wordSetId) {
        if (!wordSetRepository.existsById(wordSetId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Word set not found: " + wordSetId);
        }
    }

    private void validateHeaders(List<String> headers) {
        List<String> normalized = headers.stream()
                .map(header -> normalize(header).toLowerCase(Locale.ROOT))
                .toList();
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!normalized.contains(requiredHeader)) {
                throw new BusinessException(ErrorCode.IMPORT_ERROR, "Missing CSV header: " + requiredHeader);
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private List<String> parseTags(String rawTags) {
        String normalized = normalize(rawTags);
        if (normalized.isBlank()) {
            return List.of();
        }

        return List.of(normalized.split("[,;|，、]"))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private String buildDedupKey(String expression, String reading) {
        return normalize(expression).toLowerCase(Locale.ROOT) + "::" + normalize(reading).toLowerCase(Locale.ROOT);
    }
}
