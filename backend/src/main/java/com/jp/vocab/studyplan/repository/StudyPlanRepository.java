package com.jp.vocab.studyplan.repository;

import com.jp.vocab.studyplan.entity.StudyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, Long> {
}
