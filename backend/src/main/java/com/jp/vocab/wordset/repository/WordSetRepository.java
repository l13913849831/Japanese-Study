package com.jp.vocab.wordset.repository;

import com.jp.vocab.wordset.entity.WordSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WordSetRepository extends JpaRepository<WordSetEntity, Long> {

    @Query("""
            select ws
            from WordSetEntity ws
            where ws.scope = :systemScope or ws.ownerUserId = :userId
            """)
    Page<WordSetEntity> findAccessible(
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            select ws
            from WordSetEntity ws
            where ws.id = :id and (ws.scope = :systemScope or ws.ownerUserId = :userId)
            """)
    Optional<WordSetEntity> findAccessibleById(
            @Param("id") Long id,
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId
    );

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);
}
