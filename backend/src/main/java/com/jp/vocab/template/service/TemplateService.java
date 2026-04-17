package com.jp.vocab.template.service;

import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.template.dto.AnkiTemplateResponse;
import com.jp.vocab.template.dto.AnkiTemplatePreviewRequest;
import com.jp.vocab.template.dto.AnkiTemplatePreviewResponse;
import com.jp.vocab.template.dto.MarkdownTemplateResponse;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewRequest;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewResponse;
import com.jp.vocab.template.dto.SaveAnkiTemplateRequest;
import com.jp.vocab.template.dto.SaveMarkdownTemplateRequest;
import com.jp.vocab.template.dto.TemplateCardSample;
import com.jp.vocab.template.entity.AnkiTemplateEntity;
import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import com.jp.vocab.shared.template.SimpleTemplateEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TemplateService {

    private final AnkiTemplateRepository ankiTemplateRepository;
    private final MarkdownTemplateRepository markdownTemplateRepository;
    private final SimpleTemplateEngine templateEngine;

    public TemplateService(
            AnkiTemplateRepository ankiTemplateRepository,
            MarkdownTemplateRepository markdownTemplateRepository,
            SimpleTemplateEngine templateEngine
    ) {
        this.ankiTemplateRepository = ankiTemplateRepository;
        this.markdownTemplateRepository = markdownTemplateRepository;
        this.templateEngine = templateEngine;
    }

    @Transactional(readOnly = true)
    public List<AnkiTemplateResponse> listAnkiTemplates() {
        return ankiTemplateRepository.findAll()
                .stream()
                .map(AnkiTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarkdownTemplateResponse> listMarkdownTemplates() {
        return markdownTemplateRepository.findAll()
                .stream()
                .map(MarkdownTemplateResponse::from)
                .toList();
    }

    @Transactional
    public AnkiTemplateResponse createAnkiTemplate(SaveAnkiTemplateRequest request) {
        String normalizedName = normalizeRequired(request.name(), "name");
        ensureAnkiTemplateNameAvailable(normalizedName, null);
        validateAnkiTemplates(request.frontTemplate(), request.backTemplate());

        AnkiTemplateEntity saved = ankiTemplateRepository.save(AnkiTemplateEntity.create(
                normalizedName,
                normalizeOptional(request.description()),
                copyFieldMapping(request.fieldMapping()),
                request.frontTemplate().trim(),
                request.backTemplate().trim(),
                normalizeOptional(request.cssTemplate())
        ));
        return AnkiTemplateResponse.from(saved);
    }

    @Transactional
    public AnkiTemplateResponse updateAnkiTemplate(Long id, SaveAnkiTemplateRequest request) {
        AnkiTemplateEntity entity = ankiTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Anki template not found: " + id));

        String normalizedName = normalizeRequired(request.name(), "name");
        ensureAnkiTemplateNameAvailable(normalizedName, id);
        validateAnkiTemplates(request.frontTemplate(), request.backTemplate());

        entity.update(
                normalizedName,
                normalizeOptional(request.description()),
                copyFieldMapping(request.fieldMapping()),
                request.frontTemplate().trim(),
                request.backTemplate().trim(),
                normalizeOptional(request.cssTemplate())
        );
        return AnkiTemplateResponse.from(ankiTemplateRepository.save(entity));
    }

    @Transactional
    public MarkdownTemplateResponse createMarkdownTemplate(SaveMarkdownTemplateRequest request) {
        String normalizedName = normalizeRequired(request.name(), "name");
        ensureMarkdownTemplateNameAvailable(normalizedName, null);
        validateMarkdownTemplate(request.templateContent());

        MarkdownTemplateEntity saved = markdownTemplateRepository.save(MarkdownTemplateEntity.create(
                normalizedName,
                normalizeOptional(request.description()),
                request.templateContent().trim()
        ));
        return MarkdownTemplateResponse.from(saved);
    }

    @Transactional
    public MarkdownTemplateResponse updateMarkdownTemplate(Long id, SaveMarkdownTemplateRequest request) {
        MarkdownTemplateEntity entity = markdownTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Markdown template not found: " + id));

        String normalizedName = normalizeRequired(request.name(), "name");
        ensureMarkdownTemplateNameAvailable(normalizedName, id);
        validateMarkdownTemplate(request.templateContent());

        entity.update(
                normalizedName,
                normalizeOptional(request.description()),
                request.templateContent().trim()
        );
        return MarkdownTemplateResponse.from(markdownTemplateRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AnkiTemplatePreviewResponse previewAnki(AnkiTemplatePreviewRequest request) {
        validateAnkiTemplates(request.frontTemplate(), request.backTemplate());

        Map<String, Object> context = toScalarContext(request.sample());
        return new AnkiTemplatePreviewResponse(
                templateEngine.render(request.frontTemplate(), context),
                templateEngine.render(request.backTemplate(), context),
                request.cssTemplate() == null ? "" : request.cssTemplate()
        );
    }

    @Transactional(readOnly = true)
    public MarkdownTemplatePreviewResponse previewMarkdown(MarkdownTemplatePreviewRequest request) {
        validateMarkdownTemplate(request.templateContent());

        Map<String, Object> context = Map.of(
                "date", request.date(),
                "planName", request.planName(),
                "newCards", request.newCards().stream().map(this::toScalarContext).toList(),
                "reviewCards", request.reviewCards().stream().map(this::toScalarContext).toList()
        );

        return new MarkdownTemplatePreviewResponse(templateEngine.render(request.templateContent(), context));
    }

    private Set<String> allowedScalarVariables() {
        return Set.of("expression", "reading", "meaning", "partOfSpeech", "exampleJp", "exampleZh", "tags", "dueDate", "planName");
    }

    private Set<String> allowedMarkdownVariables() {
        return Set.of("date", "planName", "expression", "reading", "meaning", "partOfSpeech", "exampleJp", "exampleZh", "tags", "dueDate");
    }

    private void validateAnkiTemplates(String frontTemplate, String backTemplate) {
        templateEngine.validate(frontTemplate, allowedScalarVariables(), Set.of());
        templateEngine.validate(backTemplate, allowedScalarVariables(), Set.of());
    }

    private void validateMarkdownTemplate(String templateContent) {
        templateEngine.validate(
                templateContent,
                allowedMarkdownVariables(),
                Set.of("newCards", "reviewCards")
        );
    }

    private void ensureAnkiTemplateNameAvailable(String name, Long currentId) {
        boolean exists = currentId == null
                ? ankiTemplateRepository.existsByName(name)
                : ankiTemplateRepository.existsByNameAndIdNot(name, currentId);
        if (exists) {
            throw new BusinessException(ErrorCode.CONFLICT, "Anki template name already exists: " + name);
        }
    }

    private void ensureMarkdownTemplateNameAvailable(String name, Long currentId) {
        boolean exists = currentId == null
                ? markdownTemplateRepository.existsByName(name)
                : markdownTemplateRepository.existsByNameAndIdNot(name, currentId);
        if (exists) {
            throw new BusinessException(ErrorCode.CONFLICT, "Markdown template name already exists: " + name);
        }
    }

    private Map<String, List<String>> copyFieldMapping(Map<String, List<String>> fieldMapping) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : fieldMapping.entrySet()) {
            String key = normalizeRequired(entry.getKey(), "fieldMapping key");
            List<String> values = entry.getValue() == null
                    ? List.of()
                    : entry.getValue().stream()
                    .map(value -> normalizeRequired(value, "fieldMapping value"))
                    .toList();
            copied.put(key, values);
        }
        return copied;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " must not be blank");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<String, Object> toScalarContext(TemplateCardSample sample) {
        return Map.of(
                "expression", defaultString(sample.expression()),
                "reading", defaultString(sample.reading()),
                "meaning", defaultString(sample.meaning()),
                "partOfSpeech", defaultString(sample.partOfSpeech()),
                "exampleJp", defaultString(sample.exampleJp()),
                "exampleZh", defaultString(sample.exampleZh()),
                "tags", sample.tags() == null ? "" : String.join(", ", sample.tags()),
                "dueDate", defaultString(sample.dueDate()),
                "planName", defaultString(sample.planName())
        );
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
