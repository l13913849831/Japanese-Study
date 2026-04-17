package com.jp.vocab.studyplan.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.card.service.CardGenerationService;
import com.jp.vocab.studyplan.dto.CreateStudyPlanRequest;
import com.jp.vocab.studyplan.dto.StudyPlanResponse;
import com.jp.vocab.studyplan.dto.UpdateStudyPlanRequest;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import com.jp.vocab.template.repository.AnkiTemplateRepository;
import com.jp.vocab.template.repository.MarkdownTemplateRepository;
import com.jp.vocab.wordset.repository.WordSetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final WordSetRepository wordSetRepository;
    private final AnkiTemplateRepository ankiTemplateRepository;
    private final MarkdownTemplateRepository markdownTemplateRepository;
    private final CardGenerationService cardGenerationService;

    public StudyPlanService(
            StudyPlanRepository studyPlanRepository,
            WordSetRepository wordSetRepository,
            AnkiTemplateRepository ankiTemplateRepository,
            MarkdownTemplateRepository markdownTemplateRepository,
            CardGenerationService cardGenerationService
    ) {
        this.studyPlanRepository = studyPlanRepository;
        this.wordSetRepository = wordSetRepository;
        this.ankiTemplateRepository = ankiTemplateRepository;
        this.markdownTemplateRepository = markdownTemplateRepository;
        this.cardGenerationService = cardGenerationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyPlanResponse> list(int page, int pageSize) {
        return PageResponse.from(
                studyPlanRepository.findAll(
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "id")))
                        .map(StudyPlanResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse get(Long id) {
        return StudyPlanResponse.from(getEntity(id));
    }

    @Transactional
    public StudyPlanResponse create(CreateStudyPlanRequest request) {
        validateRequest(
                request.wordSetId(),
                request.reviewOffsets(),
                request.ankiTemplateId(),
                request.mdTemplateId()
        );

        StudyPlanEntity entity = StudyPlanEntity.create(
                request.name().trim(),
                request.wordSetId(),
                request.startDate(),
                request.dailyNewCount(),
                List.copyOf(request.reviewOffsets()),
                request.ankiTemplateId(),
                request.mdTemplateId()
        );

        StudyPlanEntity saved = studyPlanRepository.save(entity);
        cardGenerationService.regenerateForPlan(saved);
        return StudyPlanResponse.from(saved);
    }

    @Transactional
    public StudyPlanResponse update(Long id, UpdateStudyPlanRequest request) {
        StudyPlanEntity entity = getEntity(id);
        validateEditable(entity);
        validateRequest(
                request.wordSetId(),
                request.reviewOffsets(),
                request.ankiTemplateId(),
                request.mdTemplateId()
        );

        entity.update(
                request.name().trim(),
                request.wordSetId(),
                request.startDate(),
                request.dailyNewCount(),
                List.copyOf(request.reviewOffsets()),
                request.ankiTemplateId(),
                request.mdTemplateId()
        );

        StudyPlanEntity saved = studyPlanRepository.save(entity);
        cardGenerationService.regenerateForPlan(saved);
        return StudyPlanResponse.from(saved);
    }

    @Transactional
    public StudyPlanResponse activate(Long id) {
        StudyPlanEntity entity = getEntity(id);
        if (!entity.canActivate()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only draft or paused study plans can be activated");
        }

        entity.activate();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    @Transactional
    public StudyPlanResponse pause(Long id) {
        StudyPlanEntity entity = getEntity(id);
        if (!entity.canPause()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only active study plans can be paused");
        }

        entity.pause();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    @Transactional
    public StudyPlanResponse archive(Long id) {
        StudyPlanEntity entity = getEntity(id);
        if (!entity.canArchive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Archived study plans cannot be archived again");
        }

        entity.archive();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    private StudyPlanEntity getEntity(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Study plan not found: " + id));
    }

    private void validateRequest(
            Long wordSetId,
            List<Integer> reviewOffsets,
            Long ankiTemplateId,
            Long mdTemplateId
    ) {
        if (!wordSetRepository.existsById(wordSetId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Word set not found: " + wordSetId);
        }
        if (ankiTemplateId != null && !ankiTemplateRepository.existsById(ankiTemplateId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Anki template not found: " + ankiTemplateId);
        }
        if (mdTemplateId != null && !markdownTemplateRepository.existsById(mdTemplateId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Markdown template not found: " + mdTemplateId);
        }
        if (reviewOffsets.isEmpty() || reviewOffsets.getFirst() != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reviewOffsets must start with 0");
        }
        for (int index = 1; index < reviewOffsets.size(); index++) {
            if (reviewOffsets.get(index) < reviewOffsets.get(index - 1)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reviewOffsets must be sorted ascending");
            }
        }
    }

    private void validateEditable(StudyPlanEntity entity) {
        if (!entity.isEditable()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only draft or paused study plans can be edited");
        }
    }
}
