package com.jp.vocab.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "security_audit_event")
public class SecurityAuditEventEntity {

    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String EVENT_LOGIN_LOCKED = "LOGIN_LOCKED";
    public static final String EVENT_LOGIN_DISABLED = "LOGIN_DISABLED";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_ADMIN_USER_DETAIL_VIEW = "ADMIN_USER_DETAIL_VIEW";
    public static final String EVENT_ADMIN_USER_STATUS_CHANGE = "ADMIN_USER_STATUS_CHANGE";
    public static final String EVENT_ADMIN_USER_PASSWORD_RESET = "ADMIN_USER_PASSWORD_RESET";

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";
    public static final String OUTCOME_BLOCKED = "BLOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SecurityAuditEventEntity() {
    }

    public static SecurityAuditEventEntity create(
            String eventType,
            String outcome,
            Long userId,
            String username,
            String ipAddress,
            String userAgent,
            String message
    ) {
        SecurityAuditEventEntity entity = new SecurityAuditEventEntity();
        entity.eventType = eventType;
        entity.outcome = outcome;
        entity.userId = userId;
        entity.username = username;
        entity.ipAddress = ipAddress;
        entity.userAgent = userAgent;
        entity.message = message;
        entity.createdAt = OffsetDateTime.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
