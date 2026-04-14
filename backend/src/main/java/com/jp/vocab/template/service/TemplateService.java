package com.jp.vocab.template.service;

import com.jp.vocab.template.dto.AnkiTemplateResponse;
import com.jp.vocab.template.dto.MarkdownTemplateResponse;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final AnkiTemplateRepository ankiTemplateRepository;
    private final MarkdownTemplateRepository markdownTemplateRepository;

    public TemplateService(
            AnkiTemplateRepository ankiTemplateRepository,
            MarkdownTemplateRepository markdownTemplateRepository
    ) {
        this.ankiTemplateRepository = ankiTemplateRepository;
        this.markdownTemplateRepository = markdownTemplateRepository;
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
}
