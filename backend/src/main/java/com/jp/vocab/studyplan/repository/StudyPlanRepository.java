package com.jp.vocab.studyplan.repository;

import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, Long> {

    Page<StudyPlanEntity> findByUserId(Long userId, Pageable pageable);

    Optional<StudyPlanEntity> findByIdAndUserId(Long id, Long userId);

    List<StudyPlanEntity> findByUserIdOrderByIdAsc(Long userId);
}
