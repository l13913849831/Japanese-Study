package com.jp.vocab.template.repository;

import com.jp.vocab.template.entity.MarkdownTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarkdownTemplateRepository extends JpaRepository<MarkdownTemplateEntity, Long> {

    @Query("""
            select template
            from MarkdownTemplateEntity template
            where template.scope = :systemScope or template.ownerUserId = :userId
            order by template.id asc
            """)
    List<MarkdownTemplateEntity> findAccessible(
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId
    );

    @Query("""
            select template
            from MarkdownTemplateEntity template
            where template.id = :id and (template.scope = :systemScope or template.ownerUserId = :userId)
            """)
    Optional<MarkdownTemplateEntity> findAccessibleById(
            @Param("id") Long id,
            @Param("systemScope") String systemScope,
            @Param("userId") Long userId
    );

    Optional<MarkdownTemplateEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Optional<MarkdownTemplateEntity> findFirstByScopeOrderByIdAsc(String scope);

    List<MarkdownTemplateEntity> findByOwnerUserIdOrderByIdAsc(Long ownerUserId);

    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name);

    boolean existsByOwnerUserIdAndNameAndIdNot(Long ownerUserId, String name, Long id);
}
