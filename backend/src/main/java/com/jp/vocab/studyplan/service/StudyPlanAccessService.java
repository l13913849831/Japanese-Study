package com.jp.vocab.studyplan.service;

import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanAccessService {

    private final StudyPlanRepository studyPlanRepository;
    private final CurrentUserService currentUserService;

    public StudyPlanAccessService(StudyPlanRepository studyPlanRepository, CurrentUserService currentUserService) {
        this.studyPlanRepository = studyPlanRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public StudyPlanEntity getOwnedPlan(Long planId) {
        return studyPlanRepository.findByIdAndUserId(planId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Study plan not found: " + planId));
    }
}
