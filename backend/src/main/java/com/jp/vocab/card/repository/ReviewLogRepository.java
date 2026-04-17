package com.jp.vocab.card.repository;

import com.jp.vocab.card.entity.ReviewLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLogEntity, Long> {

    List<ReviewLogEntity> findByCardInstanceIdOrderByReviewedAtDesc(Long cardInstanceId);
}
