package com.jp.vocab.card.repository;

import com.jp.vocab.card.entity.CardInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardInstanceRepository extends JpaRepository<CardInstanceEntity, Long> {

    void deleteByPlanId(Long planId);

    @Query("""
            select card
            from CardInstanceEntity card
            where card.id = :cardId
              and exists (
                select 1
                from StudyPlanEntity plan
                where plan.id = card.planId
                  and plan.userId = :userId
            )
            """)
    Optional<CardInstanceEntity> findOwnedById(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId
    );

    List<CardInstanceEntity> findByPlanIdInOrderByPlanIdAscSequenceNoAscStageNoAscIdAsc(List<Long> planIds);
}
