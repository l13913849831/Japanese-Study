package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEventEntity, Long> {

    @Query("""
            select event
            from SecurityAuditEventEntity event
            where (:eventType is null or event.eventType = :eventType)
                and (:outcome is null or event.outcome = :outcome)
                and (:username is null or lower(event.username) like :username)
            """)
    Page<SecurityAuditEventEntity> searchAdminAuditEvents(
            @Param("eventType") String eventType,
            @Param("outcome") String outcome,
            @Param("username") String username,
            Pageable pageable
    );
}
