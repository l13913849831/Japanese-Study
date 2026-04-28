package com.jp.vocab.template.repository;

import com.jp.vocab.template.entity.AnkiTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnkiTemplateRepository extends JpaRepository<AnkiTemplateEntity, Long> {

    @Query("""
            select template
            from AnkiTemplateEntity template
            where template.scope = :systemScope or template.ownerUserId = :userId
            order by template.id asc
            """)
    List<AnkiTemplateEntity> findAccessible(
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId
    );

    @Query("""
            select template
            from AnkiTemplateEntity template
            where template.id = :id and (template.scope = :systemScope or template.ownerUserId = :userId)
            """)
    Optional<AnkiTemplateEntity> findAccessibleById(
            @Param("id") Long id,
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId
    );

    Optional<AnkiTemplateEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}
