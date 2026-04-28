package com.jp.vocab.template.service;

import com.jp.vocab.shared.auth.ContentScope;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.template.entity.AnkiTemplateEntity;
import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateAccessService {

    private final AnkiTemplateRepository ankiTemplateRepository;
    private final MarkdownTemplateRepository markdownTemplateRepository;
    private final CurrentUserService currentUserService;

    public TemplateAccessService(
            AnkiTemplateRepository ankiTemplateRepository,
            MarkdownTemplateRepository markdownTemplateRepository,
            CurrentUserService currentUserService
    ) {
        this.ankiTemplateRepository = ankiTemplateRepository;
        this.markdownTemplateRepository = markdownTemplateRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AnkiTemplateEntity getAccessibleAnkiTemplate(Long templateId) {
        return ankiTemplateRepository.findAccessibleById(templateId, ContentScope.SYSTEM, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Anki template not found: " + templateId));
    }

    @Transactional(readOnly = true)
    public AnkiTemplateEntity getEditableAnkiTemplate(Long templateId) {
        return ankiTemplateRepository.findByIdAndOwnerUserId(templateId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Only user-owned Anki templates can be edited"));
    }

    @Transactional(readOnly = true)
    public MarkdownTemplateEntity getAccessibleMarkdownTemplate(Long templateId) {
        return markdownTemplateRepository.findAccessibleById(templateId, ContentScope.SYSTEM, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Markdown template not found: " + templateId));
    }

    @Transactional(readOnly = true)
    public MarkdownTemplateEntity getEditableMarkdownTemplate(Long templateId) {
        return markdownTemplateRepository.findByIdAndOwnerUserId(templateId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Only user-owned Markdown templates can be edited"));
    }

    @Transactional(readOnly = true)
    public MarkdownTemplateEntity getDefaultMarkdownTemplate() {
        return markdownTemplateRepository.findFirstByScopeOrderByIdAsc(ContentScope.SYSTEM)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Markdown template not found"));
    }
}
