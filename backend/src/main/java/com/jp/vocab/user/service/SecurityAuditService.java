package com.jp.vocab.user.service;

import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.SecurityAuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditService {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_MESSAGE_LENGTH = 512;

    private final SecurityAuditEventRepository securityAuditEventRepository;

    public SecurityAuditService(SecurityAuditEventRepository securityAuditEventRepository) {
        this.securityAuditEventRepository = securityAuditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(HttpServletRequest request, Long userId, String username) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_LOGIN_SUCCESS,
                SecurityAuditEventEntity.OUTCOME_SUCCESS,
                userId,
                username,
                "Login succeeded"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(HttpServletRequest request, UserIdentityEntity identity, String username) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_LOGIN_FAILURE,
                SecurityAuditEventEntity.OUTCOME_FAILURE,
                userIdOf(identity),
                username,
                "Invalid username or password"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginLocked(HttpServletRequest request, UserIdentityEntity identity, String username) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_LOGIN_LOCKED,
                SecurityAuditEventEntity.OUTCOME_BLOCKED,
                userIdOf(identity),
                username,
                "User account is temporarily locked"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginDisabled(HttpServletRequest request, UserIdentityEntity identity, String username) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_LOGIN_DISABLED,
                SecurityAuditEventEntity.OUTCOME_BLOCKED,
                userIdOf(identity),
                username,
                "User account is disabled"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(HttpServletRequest request, Long userId, String username) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_LOGOUT,
                SecurityAuditEventEntity.OUTCOME_SUCCESS,
                userId,
                username,
                "Logout succeeded"
        );
    }

    private void saveEvent(
            HttpServletRequest request,
            String eventType,
            String outcome,
            Long userId,
            String username,
            String message
    ) {
        securityAuditEventRepository.save(SecurityAuditEventEntity.create(
                eventType,
                outcome,
                userId,
                truncate(username, 255),
                extractIpAddress(request),
                extractUserAgent(request),
                truncate(message, MAX_MESSAGE_LENGTH)
        ));
    }

    private Long userIdOf(UserIdentityEntity identity) {
        return identity == null || identity.getUserAccount() == null ? null : identity.getUserAccount().getId();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstForwardedIp = forwardedFor.split(",")[0].trim();
            return truncate(firstForwardedIp, MAX_IP_LENGTH);
        }
        return truncate(request.getRemoteAddr(), MAX_IP_LENGTH);
    }

    private String extractUserAgent(HttpServletRequest request) {
        return truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
