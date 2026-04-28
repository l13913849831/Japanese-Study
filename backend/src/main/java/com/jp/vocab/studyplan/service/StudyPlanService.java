package com.jp.vocab.studyplan.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.card.service.CardGenerationService;
import com.jp.vocab.studyplan.dto.CreateStudyPlanRequest;
import com.jp.vocab.studyplan.dto.StudyPlanResponse;
import com.jp.vocab.studyplan.dto.UpdateStudyPlanRequest;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import com.jp.vocab.template.service.TemplateAccessService;
import com.jp.vocab.wordset.service.WordSetAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CurrentUserService currentUserService;
    private final StudyPlanAccessService studyPlanAccessService;
    private final WordSetAccessService wordSetAccessService;
    private final TemplateAccessService templateAccessService;
    private final CardGenerationService cardGenerationService;

    public StudyPlanService(
            StudyPlanRepository studyPlanRepository,
            CurrentUserService currentUserService,
            StudyPlanAccessService studyPlanAccessService,
            WordSetAccessService wordSetAccessService,
            TemplateAccessService templateAccessService,
            CardGenerationService cardGenerationService
    ) {
        this.studyPlanRepository = studyPlanRepository;
        this.currentUserService = currentUserService;
        this.studyPlanAccessService = studyPlanAccessService;
        this.wordSetAccessService = wordSetAccessService;
        this.templateAccessService = templateAccessService;
        this.cardGenerationService = cardGenerationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyPlanResponse> list(int page, int pageSize) {
        Long userId = currentUserService.getCurrentUserId();
        return PageResponse.from(
                studyPlanRepository.findByUserId(
                                userId,
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "id")))
                        .map(StudyPlanResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse get(Long id) {
        return StudyPlanResponse.from(studyPlanAccessService.getOwnedPlan(id));
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
                currentUserService.getCurrentUserId(),
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
        StudyPlanEntity entity = studyPlanAccessService.getOwnedPlan(id);
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
        StudyPlanEntity entity = studyPlanAccessService.getOwnedPlan(id);
        if (!entity.canActivate()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only draft or paused study plans can be activated");
        }

        entity.activate();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    @Transactional
    public StudyPlanResponse pause(Long id) {
        StudyPlanEntity entity = studyPlanAccessService.getOwnedPlan(id);
        if (!entity.canPause()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only active study plans can be paused");
        }

        entity.pause();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    @Transactional
    public StudyPlanResponse archive(Long id) {
        StudyPlanEntity entity = studyPlanAccessService.getOwnedPlan(id);
        if (!entity.canArchive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Archived study plans cannot be archived again");
        }

        entity.archive();
        return StudyPlanResponse.from(studyPlanRepository.save(entity));
    }

    private void validateRequest(
            Long wordSetId,
            List<Integer> reviewOffsets,
            Long ankiTemplateId,
            Long mdTemplateId
    ) {
        wordSetAccessService.getAccessibleWordSet(wordSetId);
        if (ankiTemplateId != null) {
            templateAccessService.getAccessibleAnkiTemplate(ankiTemplateId);
        }
        if (mdTemplateId != null) {
            templateAccessService.getAccessibleMarkdownTemplate(mdTemplateId);
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
