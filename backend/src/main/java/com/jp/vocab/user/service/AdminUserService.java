package com.jp.vocab.user.service;

import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.AdminResetPasswordRequest;
import com.jp.vocab.user.dto.AdminUserAssetSummaryResponse;
import com.jp.vocab.user.dto.AdminUserDetailResponse;
import com.jp.vocab.user.dto.AdminUserListItemResponse;
import com.jp.vocab.user.dto.AdminUserPasswordResetResponse;
import com.jp.vocab.user.dto.AdminUserStatusResponse;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final SecurityAuditService securityAuditService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            NamedParameterJdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService,
            SecurityAuditService securityAuditService,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.securityAuditService = securityAuditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItemResponse> listUsers(
            int page,
            int pageSize,
            String keyword,
            String status,
            String role
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        Page<UserAccountEntity> accountPage = userAccountRepository.searchAdminUsers(
                normalizeKeyword(keyword),
                normalizeStatusFilter(status),
                normalizeRoleFilter(role),
                PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "id"))
        );
        Map<Long, UserIdentityEntity> identities = findLocalIdentities(accountPage.getContent());
        return new PageResponse<>(
                accountPage.getContent()
                        .stream()
                        .map(account -> toListItem(account, identities.get(account.getId())))
                        .toList(),
                safePage,
                safePageSize,
                accountPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId, HttpServletRequest request) {
        UserAccountEntity account = getUserAccount(userId);
        UserIdentityEntity identity = userIdentityRepository.findByUserAccountIdAndProvider(userId, AuthProvider.LOCAL)
                .orElse(null);
        AppUserPrincipal admin = currentUserService.getCurrentUser();
        securityAuditService.recordAdminUserDetailView(request, admin.getUserId(), admin.getUsername(), userId);
        return new AdminUserDetailResponse(
                account.getId(),
                usernameOf(identity),
                account.getDisplayName(),
                account.getStatus(),
                account.getRole(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                loadAssetSummary(userId)
        );
    }

    @Transactional
    public AdminUserStatusResponse disableUser(Long userId, HttpServletRequest request) {
        AppUserPrincipal admin = currentUserService.getCurrentUser();
        if (admin.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Administrators cannot disable their own account");
        }
        UserAccountEntity account = getUserAccount(userId);
        account.disable();
        userAccountRepository.save(account);
        securityAuditService.recordAdminUserStatusChange(
                request,
                admin.getUserId(),
                admin.getUsername(),
                userId,
                UserAccountEntity.STATUS_DISABLED
        );
        return new AdminUserStatusResponse(account.getId(), account.getStatus());
    }

    @Transactional
    public AdminUserPasswordResetResponse resetUserPassword(
            Long userId,
            AdminResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request
    ) {
        AppUserPrincipal admin = currentUserService.getCurrentUser();
        if (admin.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Administrators cannot reset their own password from admin console");
        }
        UserIdentityEntity identity = userIdentityRepository.findByUserAccountIdAndProvider(userId, AuthProvider.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Local identity not found for user: " + userId));
        validateNewPassword(resetPasswordRequest.newPassword());
        if (passwordEncoder.matches(resetPasswordRequest.newPassword(), identity.getPasswordHash())) {
            throw new BusinessException(ErrorCode.CONFLICT, "New password must be different from current password");
        }
        identity.updatePasswordHash(passwordEncoder.encode(resetPasswordRequest.newPassword()));
        identity.clearLoginFailureState();
        userIdentityRepository.save(identity);
        securityAuditService.recordAdminUserPasswordReset(request, admin.getUserId(), admin.getUsername(), userId);
        return new AdminUserPasswordResetResponse(userId, true);
    }

    @Transactional
    public AdminUserStatusResponse enableUser(Long userId, HttpServletRequest request) {
        AppUserPrincipal admin = currentUserService.getCurrentUser();
        UserAccountEntity account = getUserAccount(userId);
        account.enable();
        userAccountRepository.save(account);
        securityAuditService.recordAdminUserStatusChange(
                request,
                admin.getUserId(),
                admin.getUsername(),
                userId,
                UserAccountEntity.STATUS_ACTIVE
        );
        return new AdminUserStatusResponse(account.getId(), account.getStatus());
    }

    private UserAccountEntity getUserAccount(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User account not found: " + userId));
    }

    private Map<Long, UserIdentityEntity> findLocalIdentities(Collection<UserAccountEntity> accounts) {
        if (accounts.isEmpty()) {
            return Map.of();
        }
        return userIdentityRepository.findByUserAccountIdInAndProvider(
                        accounts.stream().map(UserAccountEntity::getId).toList(),
                        AuthProvider.LOCAL
                )
                .stream()
                .collect(Collectors.toMap(identity -> identity.getUserAccount().getId(), Function.identity()));
    }

    private AdminUserListItemResponse toListItem(UserAccountEntity account, UserIdentityEntity identity) {
        return new AdminUserListItemResponse(
                account.getId(),
                usernameOf(identity),
                account.getDisplayName(),
                account.getStatus(),
                account.getRole(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private AdminUserAssetSummaryResponse loadAssetSummary(Long userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("userId", userId);
        return new AdminUserAssetSummaryResponse(
                count("select count(*) from word_set where owner_user_id = :userId", parameters),
                count("select count(*) from study_plan where user_id = :userId", parameters),
                count("select count(*) from note where user_id = :userId", parameters)
        );
    }

    private long count(String sql, MapSqlParameterSource parameters) {
        Long value = jdbcTemplate.queryForObject(sql, parameters, Long.class);
        return value == null ? 0 : value;
    }

    private String usernameOf(UserIdentityEntity identity) {
        return identity == null ? null : identity.getProviderSubject();
    }

    private String normalizeKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : "%" + normalized + "%";
    }

    private String normalizeStatusFilter(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (UserAccountEntity.STATUS_ACTIVE.equals(normalized) || UserAccountEntity.STATUS_DISABLED.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status is invalid");
    }

    private String normalizeRoleFilter(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (UserRole.USER.equals(normalized) || UserRole.ADMIN.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "role is invalid");
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "newPassword must not be blank");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "newPassword must be between 8 and 72 characters");
        }
    }
}
