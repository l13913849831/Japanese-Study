package com.jp.vocab.template.service;

import com.jp.vocab.template.dto.AnkiTemplateResponse;
import com.jp.vocab.template.dto.AnkiTemplatePreviewRequest;
import com.jp.vocab.template.dto.AnkiTemplatePreviewResponse;
import com.jp.vocab.template.dto.MarkdownTemplateResponse;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewRequest;
import com.jp.vocab.template.dto.MarkdownTemplatePreviewResponse;
import com.jp.vocab.template.dto.TemplateCardSample;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import com.jp.vocab.shared.template.SimpleTemplateEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public AnkiTemplatePreviewResponse previewAnki(AnkiTemplatePreviewRequest request) {
        templateEngine.validate(request.frontTemplate(), allowedScalarVariables(), Set.of());
        templateEngine.validate(request.backTemplate(), allowedScalarVariables(), Set.of());

        Map<String, Object> context = toScalarContext(request.sample());
        return new AnkiTemplatePreviewResponse(
                templateEngine.render(request.frontTemplate(), context),
                templateEngine.render(request.backTemplate(), context),
                request.cssTemplate() == null ? "" : request.cssTemplate()
        );
    }

    @Transactional(readOnly = true)
    public MarkdownTemplatePreviewResponse previewMarkdown(MarkdownTemplatePreviewRequest request) {
        templateEngine.validate(
                request.templateContent(),
                allowedMarkdownVariables(),
                Set.of("newCards", "reviewCards")
        );

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
