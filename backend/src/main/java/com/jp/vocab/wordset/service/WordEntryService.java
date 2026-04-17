package com.jp.vocab.wordset.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.csv.CsvRow;
import com.jp.vocab.shared.csv.SimpleCsvParser;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.wordset.dto.DeleteWordEntryResponse;
import com.jp.vocab.wordset.dto.SaveWordEntryRequest;
import com.jp.vocab.wordset.dto.WordEntryImportError;
import com.jp.vocab.wordset.dto.WordEntryImportResponse;
import com.jp.vocab.wordset.dto.WordEntryResponse;
import com.jp.vocab.wordset.entity.WordEntryEntity;
import com.jp.vocab.wordset.repository.WordEntryRepository;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WordEntryService {

    private static final List<String> REQUIRED_HEADERS = List.of("expression", "meaning");

    private final WordEntryRepository wordEntryRepository;
    private final WordSetRepository wordSetRepository;
    private final SimpleCsvParser csvParser;
    private final AnkiApkgImportService ankiApkgImportService;

    public WordEntryService(
            WordEntryRepository wordEntryRepository,
            WordSetRepository wordSetRepository,
            SimpleCsvParser csvParser,
            AnkiApkgImportService ankiApkgImportService
    ) {
        this.wordEntryRepository = wordEntryRepository;
        this.wordSetRepository = wordSetRepository;
        this.csvParser = csvParser;
        this.ankiApkgImportService = ankiApkgImportService;
    }

    @Transactional(readOnly = true)
    public PageResponse<WordEntryResponse> list(
            Long wordSetId,
            int page,
            int pageSize,
            String keyword,
            String level,
            String tag
    ) {
        ensureWordSetExists(wordSetId);
        List<WordEntryResponse> filteredEntries = wordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(wordSetId)
                .stream()
                .filter(entry -> matchesKeyword(entry, keyword))
                .filter(entry -> matchesLevel(entry, level))
                .filter(entry -> matchesTag(entry, tag))
                .map(WordEntryResponse::from)
                .toList();

        if (filteredEntries.isEmpty()) {
            return PageResponse.empty(page, pageSize);
        }

        int normalizedPage = Math.max(page, 1);
        int startIndex = Math.min((normalizedPage - 1) * pageSize, filteredEntries.size());
        int endIndex = Math.min(startIndex + pageSize, filteredEntries.size());
        return new PageResponse<>(
                filteredEntries.subList(startIndex, endIndex),
                normalizedPage,
                pageSize,
                filteredEntries.size()
        );
    }

    @Transactional
    public WordEntryResponse create(Long wordSetId, SaveWordEntryRequest request) {
        ensureWordSetExists(wordSetId);

        SanitizedWordEntry sanitized = sanitize(request);
        ensureNoDuplicate(wordSetId, sanitized.expression(), sanitized.reading(), null);

        int nextSourceOrder = wordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(wordSetId)
                .stream()
                .map(WordEntryEntity::getSourceOrder)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        WordEntryEntity saved = wordEntryRepository.save(WordEntryEntity.create(
                wordSetId,
                sanitized.expression(),
                sanitized.reading(),
                sanitized.meaning(),
                sanitized.partOfSpeech(),
                sanitized.exampleJp(),
                sanitized.exampleZh(),
                sanitized.level(),
                sanitized.tags(),
                nextSourceOrder
        ));
        return WordEntryResponse.from(saved);
    }

    @Transactional
    public WordEntryResponse update(Long wordId, SaveWordEntryRequest request) {
        WordEntryEntity entity = getEntity(wordId);
        SanitizedWordEntry sanitized = sanitize(request);
        ensureNoDuplicate(entity.getWordSetId(), sanitized.expression(), sanitized.reading(), entity.getId());

        entity.update(
                sanitized.expression(),
                sanitized.reading(),
                sanitized.meaning(),
                sanitized.partOfSpeech(),
                sanitized.exampleJp(),
                sanitized.exampleZh(),
                sanitized.level(),
                sanitized.tags()
        );

        return WordEntryResponse.from(wordEntryRepository.save(entity));
    }

    @Transactional
    public DeleteWordEntryResponse delete(Long wordId) {
        WordEntryEntity entity = getEntity(wordId);
        wordEntryRepository.delete(entity);
        return new DeleteWordEntryResponse(true);
    }

    @Transactional
    public WordEntryImportResponse importEntries(Long wordSetId, MultipartFile file) {
        ensureWordSetExists(wordSetId);
        validateUpload(file);

        List<ParsedWordEntryRow> rows = isApkg(file)
                ? ankiApkgImportService.parse(file)
                : parseCsvRows(file);

        return persistRows(wordSetId, rows);
    }

    private List<ParsedWordEntryRow> parseCsvRows(MultipartFile file) {
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Failed to read CSV file");
        }

        List<CsvRow> rows = csvParser.parse(content);
        if (rows.isEmpty()) {
            return List.of();
        }

        validateHeaders(rows.getFirst().values().keySet().stream().toList());
        return rows.stream()
                .map(row -> new ParsedWordEntryRow(row.lineNumber(), normalizeKeys(row.values())))
                .toList();
    }

    private WordEntryImportResponse persistRows(Long wordSetId, List<ParsedWordEntryRow> rows) {
        if (rows.isEmpty()) {
            return new WordEntryImportResponse(0, 0, List.of());
        }

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

        for (ParsedWordEntryRow row : rows) {
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

    private WordEntryEntity getEntity(Long wordId) {
        return wordEntryRepository.findById(wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Word entry not found: " + wordId));
    }

    private SanitizedWordEntry sanitize(SaveWordEntryRequest request) {
        String expression = normalize(request.expression());
        String meaning = normalize(request.meaning());

        if (expression.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "expression must not be blank");
        }
        if (meaning.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "meaning must not be blank");
        }

        return new SanitizedWordEntry(
                expression,
                nullable(request.reading()),
                meaning,
                nullable(request.partOfSpeech()),
                nullable(request.exampleJp()),
                nullable(request.exampleZh()),
                nullable(request.level()),
                normalizeTags(request.tags())
        );
    }

    private void ensureNoDuplicate(Long wordSetId, String expression, String reading, Long excludedWordId) {
        String candidateKey = buildDedupKey(expression, reading);
        boolean duplicated = wordEntryRepository.findByWordSetIdOrderBySourceOrderAsc(wordSetId)
                .stream()
                .anyMatch(entry -> !entry.getId().equals(excludedWordId)
                        && buildDedupKey(entry.getExpression(), entry.getReading()).equals(candidateKey));

        if (duplicated) {
            throw new BusinessException(ErrorCode.CONFLICT, "Word entry already exists in this word set");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_ERROR, "Import file must not be empty");
        }
    }

    private void validateHeaders(List<String> headers) {
        List<String> normalized = headers.stream()
                .map(this::normalizeHeader)
                .toList();
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!normalized.contains(requiredHeader)) {
                throw new BusinessException(ErrorCode.IMPORT_ERROR, "Missing CSV header: " + requiredHeader);
            }
        }
    }

    private Map<String, String> normalizeKeys(Map<String, String> values) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            normalized.put(normalizeHeader(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private String normalizeHeader(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private boolean isApkg(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".apkg");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(tags.stream()
                .map(this::normalize)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private List<String> parseTags(String rawTags) {
        String normalized = normalize(rawTags);
        if (normalized.isBlank()) {
            return List.of();
        }

        String normalizedSeparators = normalized
                .replace('\u3001', ',')
                .replace('\uFF0C', ',');

        return new ArrayList<>(new LinkedHashSet<>(
                List.of(normalizedSeparators.split("[,;|/\\s]+"))
                        .stream()
                        .map(String::trim)
                        .filter(tag -> !tag.isBlank())
                        .toList()
        ));
    }

    private String buildDedupKey(String expression, String reading) {
        return normalize(expression).toLowerCase(Locale.ROOT) + "::" + normalize(reading).toLowerCase(Locale.ROOT);
    }

    private boolean matchesKeyword(WordEntryEntity entry, String keyword) {
        String normalized = normalize(keyword).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return true;
        }

        return containsIgnoreCase(entry.getExpression(), normalized)
                || containsIgnoreCase(entry.getReading(), normalized)
                || containsIgnoreCase(entry.getMeaning(), normalized)
                || containsIgnoreCase(entry.getPartOfSpeech(), normalized)
                || containsIgnoreCase(entry.getExampleJp(), normalized)
                || containsIgnoreCase(entry.getExampleZh(), normalized)
                || entry.getTags().stream().anyMatch(tagValue -> containsIgnoreCase(tagValue, normalized));
    }

    private boolean matchesLevel(WordEntryEntity entry, String level) {
        String normalized = normalize(level);
        return normalized.isBlank() || normalize(entry.getLevel()).equalsIgnoreCase(normalized);
    }

    private boolean matchesTag(WordEntryEntity entry, String tag) {
        String normalized = normalize(tag).toLowerCase(Locale.ROOT);
        return normalized.isBlank()
                || entry.getTags().stream().map(tagValue -> tagValue.toLowerCase(Locale.ROOT)).anyMatch(normalized::equals);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return normalize(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private record SanitizedWordEntry(
            String expression,
            String reading,
            String meaning,
            String partOfSpeech,
            String exampleJp,
            String exampleZh,
            String level,
            List<String> tags
    ) {
    }
}
