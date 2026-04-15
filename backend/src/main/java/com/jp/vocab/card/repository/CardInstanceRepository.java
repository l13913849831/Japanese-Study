package com.jp.vocab.card.repository;

import com.jp.vocab.card.entity.CardInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardInstanceRepository extends JpaRepository<CardInstanceEntity, Long> {

    void deleteByPlanId(Long planId);
}
