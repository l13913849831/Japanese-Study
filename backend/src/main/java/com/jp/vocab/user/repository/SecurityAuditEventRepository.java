package com.jp.vocab.user.repository;

import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

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

    @Query("""
            select event.eventType as eventType,
                event.username as username,
                event.ipAddress as ipAddress,
                count(event.id) as eventCount,
                max(event.createdAt) as lastSeenAt
            from SecurityAuditEventEntity event
            where event.eventType = :eventType
                and event.createdAt >= :since
            group by event.eventType, event.username, event.ipAddress
            having count(event.id) >= :minCount
            order by count(event.id) desc, max(event.createdAt) desc
            """)
    List<SecurityAuditAlertAggregate> aggregateRepeatedEventsSince(
            @Param("eventType") String eventType,
            @Param("since") OffsetDateTime since,
            @Param("minCount") long minCount,
            Pageable pageable
    );

    @Query("""
            select event.eventType as eventType,
                event.username as username,
                event.ipAddress as ipAddress,
                count(event.id) as eventCount,
                max(event.createdAt) as lastSeenAt
            from SecurityAuditEventEntity event
            where event.eventType in :eventTypes
                and event.createdAt >= :since
            group by event.eventType, event.username, event.ipAddress
            order by max(event.createdAt) desc
            """)
    List<SecurityAuditAlertAggregate> aggregateEventsSince(
            @Param("eventTypes") Collection<String> eventTypes,
            @Param("since") OffsetDateTime since,
            Pageable pageable
    );

    interface SecurityAuditAlertAggregate {

        String getEventType();

        String getUsername();

        String getIpAddress();

        Long getEventCount();

        OffsetDateTime getLastSeenAt();
    }
}
