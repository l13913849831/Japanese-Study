package com.jp.vocab.studyplan.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.studyplan.dto.StudyPlanResponse;
import com.jp.vocab.studyplan.repository.StudyPlanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;

    public StudyPlanService(StudyPlanRepository studyPlanRepository) {
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudyPlanResponse> list(int page, int pageSize) {
        return PageResponse.from(
                studyPlanRepository.findAll(
                                PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by(Sort.Direction.ASC, "id")))
                        .map(StudyPlanResponse::from)
        );
    }
}
