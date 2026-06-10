package com.jp.vocab.user.service;

import com.jp.vocab.shared.config.AuthLoginProperties;
import com.jp.vocab.user.dto.SecurityAlertResponse;
import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import com.jp.vocab.user.repository.SecurityAuditEventRepository;
import com.jp.vocab.user.repository.SecurityAuditEventRepository.SecurityAuditAlertAggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SecurityAlertService {

    public static final String ALERT_REPEATED_LOGIN_FAILURE = "REPEATED_LOGIN_FAILURE";
    public static final String ALERT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ALERT_DISABLED_ACCOUNT_LOGIN = "DISABLED_ACCOUNT_LOGIN";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);
    private static final int DEFAULT_LOOKBACK_HOURS = 24;
    private static final int MAX_LOOKBACK_HOURS = 168;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MIN_REPEATED_FAILURE_THRESHOLD = 3;
    private static final List<String> BLOCKED_LOGIN_EVENT_TYPES = List.of(
            SecurityAuditEventEntity.EVENT_LOGIN_LOCKED,
            SecurityAuditEventEntity.EVENT_LOGIN_DISABLED
    );

    private final SecurityAuditEventRepository securityAuditEventRepository;
    private final AuthLoginProperties authLoginProperties;

    public SecurityAlertService(
            SecurityAuditEventRepository securityAuditEventRepository,
            AuthLoginProperties authLoginProperties
    ) {
        this.securityAuditEventRepository = securityAuditEventRepository;
        this.authLoginProperties = authLoginProperties;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlertResponse> listAlerts(Integer lookbackHours, Integer limit) {
        /*
         * ========================================================================
         * Step 1: Normalize alert window
         * ========================================================================
         * Goal:
         * 1) Keep the admin query bounded.
         * 2) Reuse the login lock policy as the repeated-failure threshold.
         */
        logger.info("Starting security alert query normalization...");

        // 1.1 Clamp the lookback window and page size.
        int safeLookbackHours = clamp(lookbackHours, DEFAULT_LOOKBACK_HOURS, 1, MAX_LOOKBACK_HOURS);
        int safeLimit = clamp(limit, DEFAULT_LIMIT, 1, MAX_LIMIT);

        // 1.2 Use current login lock settings for repeated failure detection.
        int repeatedFailureThreshold = Math.max(
                MIN_REPEATED_FAILURE_THRESHOLD,
                authLoginProperties.getMaxFailedAttempts()
        );
        OffsetDateTime since = OffsetDateTime.now().minusHours(safeLookbackHours);
        Pageable queryLimit = PageRequest.of(0, safeLimit);
        logger.info(
                "Security alert query normalization completed, since={}, limit={}, repeatedFailureThreshold={}",
                since,
                safeLimit,
                repeatedFailureThreshold
        );

        /*
         * ========================================================================
         * Step 2: Query audit aggregates
         * ========================================================================
         * Data source:
         * 1) security_audit_event grouped by username and IP.
         * 2) LOGIN_FAILURE / LOGIN_LOCKED / LOGIN_DISABLED events only.
         */
        logger.info("Starting security alert aggregate query...");

        // 2.1 Find repeated failed login clusters.
        List<SecurityAuditAlertAggregate> repeatedFailures =
                securityAuditEventRepository.aggregateRepeatedEventsSince(
                        SecurityAuditEventEntity.EVENT_LOGIN_FAILURE,
                        since,
                        repeatedFailureThreshold,
                        queryLimit
                );

        // 2.2 Find lock and disabled-login clusters.
        List<SecurityAuditAlertAggregate> blockedLoginEvents =
                securityAuditEventRepository.aggregateEventsSince(BLOCKED_LOGIN_EVENT_TYPES, since, queryLimit);
        logger.info(
                "Security alert aggregate query completed, repeatedFailures={}, blockedEvents={}",
                repeatedFailures.size(),
                blockedLoginEvents.size()
        );

        /*
         * ========================================================================
         * Step 3: Build alert responses
         * ========================================================================
         * Operation:
         * 1) Convert audit aggregates to actionable alert cards.
         * 2) Sort by latest event time and apply the final limit.
         */
        logger.info("Starting security alert response mapping...");

        // 3.1 Map each aggregate into a frontend-friendly alert.
        List<SecurityAlertResponse> alerts = new ArrayList<>();
        repeatedFailures.forEach(aggregate ->
                alerts.add(toRepeatedFailureAlert(aggregate, safeLookbackHours, repeatedFailureThreshold)));
        blockedLoginEvents.forEach(aggregate ->
                alerts.add(toBlockedLoginAlert(aggregate, safeLookbackHours)));

        // 3.2 Keep the most recent alerts first.
        List<SecurityAlertResponse> response = alerts.stream()
                .sorted(Comparator.comparing(SecurityAlertResponse::lastSeenAt).reversed())
                .limit(safeLimit)
                .toList();
        logger.info("Security alert response mapping completed, alerts={}", response.size());
        return response;
    }

    private SecurityAlertResponse toRepeatedFailureAlert(
            SecurityAuditAlertAggregate aggregate,
            int lookbackHours,
            int repeatedFailureThreshold
    ) {
        long eventCount = eventCountOf(aggregate);
        String severity = eventCount >= repeatedFailureThreshold * 2L ? SEVERITY_HIGH : SEVERITY_MEDIUM;
        return new SecurityAlertResponse(
                alertId(ALERT_REPEATED_LOGIN_FAILURE, aggregate),
                ALERT_REPEATED_LOGIN_FAILURE,
                severity,
                "Repeated login failures",
                "Observed " + eventCount + " failed login attempts in the last " + lookbackHours + " hours.",
                aggregate.getUsername(),
                aggregate.getIpAddress(),
                eventCount,
                aggregate.getLastSeenAt()
        );
    }

    private SecurityAlertResponse toBlockedLoginAlert(SecurityAuditAlertAggregate aggregate, int lookbackHours) {
        if (SecurityAuditEventEntity.EVENT_LOGIN_DISABLED.equals(aggregate.getEventType())) {
            return new SecurityAlertResponse(
                    alertId(ALERT_DISABLED_ACCOUNT_LOGIN, aggregate),
                    ALERT_DISABLED_ACCOUNT_LOGIN,
                    SEVERITY_MEDIUM,
                    "Disabled account login attempt",
                    "Observed disabled-account login attempts in the last " + lookbackHours + " hours.",
                    aggregate.getUsername(),
                    aggregate.getIpAddress(),
                    eventCountOf(aggregate),
                    aggregate.getLastSeenAt()
            );
        }
        return new SecurityAlertResponse(
                alertId(ALERT_ACCOUNT_LOCKED, aggregate),
                ALERT_ACCOUNT_LOCKED,
                SEVERITY_HIGH,
                "Account lock reached",
                "Observed login attempts that hit the account lock policy in the last " + lookbackHours + " hours.",
                aggregate.getUsername(),
                aggregate.getIpAddress(),
                eventCountOf(aggregate),
                aggregate.getLastSeenAt()
        );
    }

    private String alertId(String alertType, SecurityAuditAlertAggregate aggregate) {
        return alertType
                + ":"
                + safeKeyPart(aggregate.getUsername())
                + ":"
                + safeKeyPart(aggregate.getIpAddress());
    }

    private String safeKeyPart(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private long eventCountOf(SecurityAuditAlertAggregate aggregate) {
        return aggregate.getEventCount() == null ? 0L : aggregate.getEventCount();
    }

    private int clamp(Integer value, int defaultValue, int minValue, int maxValue) {
        int normalized = value == null ? defaultValue : value;
        return Math.min(Math.max(normalized, minValue), maxValue);
    }
}
