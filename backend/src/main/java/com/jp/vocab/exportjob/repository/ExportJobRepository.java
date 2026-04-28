package com.jp.vocab.exportjob.repository;

import com.jp.vocab.exportjob.entity.ExportJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExportJobRepository extends JpaRepository<ExportJobEntity, Long> {

    @Query("""
            select exportJob
            from ExportJobEntity exportJob
            where exists (
                select 1
                from StudyPlanEntity plan
                where plan.id = exportJob.planId
                  and plan.userId = :userId
            )
            """)
    Page<ExportJobEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select exportJob
            from ExportJobEntity exportJob
            where exportJob.id = :exportJobId
              and exists (
                select 1
                from StudyPlanEntity plan
                where plan.id = exportJob.planId
                  and plan.userId = :userId
            )
            """)
    Optional<ExportJobEntity> findOwnedById(
            @Param("exportJobId") Long exportJobId,
            @Param("userId") Long userId
    );
}
