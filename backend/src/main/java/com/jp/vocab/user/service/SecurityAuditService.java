package com.jp.vocab.user.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.SecurityAuditEventResponse;
import com.jp.vocab.user.entity.SecurityAuditEventEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.SecurityAuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class SecurityAuditService {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_MESSAGE_LENGTH = 512;
    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            SecurityAuditEventEntity.EVENT_LOGIN_SUCCESS,
            SecurityAuditEventEntity.EVENT_LOGIN_FAILURE,
            SecurityAuditEventEntity.EVENT_LOGIN_LOCKED,
            SecurityAuditEventEntity.EVENT_LOGIN_DISABLED,
            SecurityAuditEventEntity.EVENT_LOGOUT,
            SecurityAuditEventEntity.EVENT_ADMIN_USER_DETAIL_VIEW,
            SecurityAuditEventEntity.EVENT_ADMIN_USER_STATUS_CHANGE,
            SecurityAuditEventEntity.EVENT_ADMIN_USER_PASSWORD_RESET
    );
    private static final Set<String> VALID_OUTCOMES = Set.of(
            SecurityAuditEventEntity.OUTCOME_SUCCESS,
            SecurityAuditEventEntity.OUTCOME_FAILURE,
            SecurityAuditEventEntity.OUTCOME_BLOCKED
    );

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAdminUserDetailView(HttpServletRequest request, Long adminUserId, String adminUsername, Long targetUserId) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_ADMIN_USER_DETAIL_VIEW,
                SecurityAuditEventEntity.OUTCOME_SUCCESS,
                adminUserId,
                adminUsername,
                "Viewed user detail: " + targetUserId
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAdminUserStatusChange(
            HttpServletRequest request,
            Long adminUserId,
            String adminUsername,
            Long targetUserId,
            String status
    ) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_ADMIN_USER_STATUS_CHANGE,
                SecurityAuditEventEntity.OUTCOME_SUCCESS,
                adminUserId,
                adminUsername,
                "Changed user status: " + targetUserId + " -> " + status
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAdminUserPasswordReset(HttpServletRequest request, Long adminUserId, String adminUsername, Long targetUserId) {
        saveEvent(
                request,
                SecurityAuditEventEntity.EVENT_ADMIN_USER_PASSWORD_RESET,
                SecurityAuditEventEntity.OUTCOME_SUCCESS,
                adminUserId,
                adminUsername,
                "Reset user password: " + targetUserId
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<SecurityAuditEventResponse> listEvents(
            int page,
            int pageSize,
            String eventType,
            String outcome,
            String username
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        Page<SecurityAuditEventEntity> auditPage = securityAuditEventRepository.searchAdminAuditEvents(
                normalizeEventType(eventType),
                normalizeOutcome(outcome),
                normalizeUsername(username),
                PageRequest.of(
                        safePage - 1,
                        safePageSize,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
                )
        );
        return new PageResponse<>(
                auditPage.getContent().stream().map(this::toResponse).toList(),
                safePage,
                safePageSize,
                auditPage.getTotalElements()
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

    private SecurityAuditEventResponse toResponse(SecurityAuditEventEntity event) {
        return new SecurityAuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getOutcome(),
                event.getUserId(),
                event.getUsername(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getMessage(),
                event.getCreatedAt()
        );
    }

    private String normalizeEventType(String eventType) {
        String normalized = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (VALID_EVENT_TYPES.contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "eventType is invalid");
    }

    private String normalizeOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (VALID_OUTCOMES.contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "outcome is invalid");
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : "%" + normalized + "%";
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
