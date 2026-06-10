package com.jp.vocab.user.service;

import com.jp.vocab.shared.config.AuthLoginProperties;
import com.jp.vocab.user.dto.SecurityAlertResponse;
import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import com.jp.vocab.user.repository.SecurityAuditEventRepository;
import com.jp.vocab.user.repository.SecurityAuditEventRepository.SecurityAuditAlertAggregate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAlertServiceTest {

    @Mock
    private SecurityAuditEventRepository securityAuditEventRepository;

    private AuthLoginProperties authLoginProperties;

    private SecurityAlertService securityAlertService;

    @BeforeEach
    void setUp() {
        authLoginProperties = new AuthLoginProperties();
        securityAlertService = new SecurityAlertService(securityAuditEventRepository, authLoginProperties);
    }

    @Test
    void shouldBuildSecurityAlertsFromAuditAggregates() {
        OffsetDateTime lockedAt = OffsetDateTime.parse("2026-06-10T09:00:00Z");
        OffsetDateTime failedAt = OffsetDateTime.parse("2026-06-10T08:00:00Z");
        when(securityAuditEventRepository.aggregateRepeatedEventsSince(
                eq(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE),
                any(OffsetDateTime.class),
                eq(5L),
                any(Pageable.class)
        )).thenReturn(List.of(new Aggregate(
                SecurityAuditEventEntity.EVENT_LOGIN_FAILURE,
                "demo",
                "127.0.0.1",
                5L,
                failedAt
        )));
        when(securityAuditEventRepository.aggregateEventsSince(
                eq(List.of(SecurityAuditEventEntity.EVENT_LOGIN_LOCKED, SecurityAuditEventEntity.EVENT_LOGIN_DISABLED)),
                any(OffsetDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(new Aggregate(
                SecurityAuditEventEntity.EVENT_LOGIN_LOCKED,
                "demo",
                "127.0.0.1",
                1L,
                lockedAt
        )));

        List<SecurityAlertResponse> response = securityAlertService.listAlerts(24, 20);

        assertEquals(2, response.size());
        assertEquals(SecurityAlertService.ALERT_ACCOUNT_LOCKED, response.get(0).alertType());
        assertEquals(SecurityAlertService.SEVERITY_HIGH, response.get(0).severity());
        assertEquals(SecurityAlertService.ALERT_REPEATED_LOGIN_FAILURE, response.get(1).alertType());
        assertEquals(SecurityAlertService.SEVERITY_MEDIUM, response.get(1).severity());
        assertEquals(5L, response.get(1).eventCount());
    }

    @Test
    void shouldClampAlertQueryParameters() {
        authLoginProperties.setMaxFailedAttempts(0);
        when(securityAuditEventRepository.aggregateRepeatedEventsSince(
                eq(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE),
                any(OffsetDateTime.class),
                eq(3L),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(securityAuditEventRepository.aggregateEventsSince(
                eq(List.of(SecurityAuditEventEntity.EVENT_LOGIN_LOCKED, SecurityAuditEventEntity.EVENT_LOGIN_DISABLED)),
                any(OffsetDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        securityAlertService.listAlerts(999, 999);

        ArgumentCaptor<Pageable> repeatedFailurePageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(securityAuditEventRepository).aggregateRepeatedEventsSince(
                eq(SecurityAuditEventEntity.EVENT_LOGIN_FAILURE),
                any(OffsetDateTime.class),
                eq(3L),
                repeatedFailurePageableCaptor.capture()
        );
        assertEquals(100, repeatedFailurePageableCaptor.getValue().getPageSize());
    }

    private record Aggregate(
            String eventType,
            String username,
            String ipAddress,
            Long eventCount,
            OffsetDateTime lastSeenAt
    ) implements SecurityAuditAlertAggregate {

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getIpAddress() {
            return ipAddress;
        }

        @Override
        public Long getEventCount() {
            return eventCount;
        }

        @Override
        public OffsetDateTime getLastSeenAt() {
            return lastSeenAt;
        }
    }
}
