package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.AdminResetPasswordRequest;
import com.jp.vocab.user.dto.AdminUserDetailResponse;
import com.jp.vocab.user.dto.AdminUserPasswordResetResponse;
import com.jp.vocab.user.dto.AdminUserStatusResponse;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private com.jp.vocab.shared.auth.CurrentUserService currentUserService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest request;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userAccountRepository,
                userIdentityRepository,
                jdbcTemplate,
                currentUserService,
                securityAuditService,
                passwordEncoder
        );
    }

    @Test
    void shouldDisableTargetUserAndRecordAudit() {
        UserAccountEntity target = createAccount(2L, "Target User");
        when(currentUserService.getCurrentUser()).thenReturn(adminPrincipal(1L));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(target));

        AdminUserStatusResponse response = adminUserService.disableUser(2L, request);

        assertEquals(2L, response.id());
        assertEquals(UserAccountEntity.STATUS_DISABLED, response.status());
        verify(userAccountRepository).save(target);
        verify(securityAuditService).recordAdminUserStatusChange(
                request,
                1L,
                "admin",
                2L,
                UserAccountEntity.STATUS_DISABLED
        );
    }

    @Test
    void shouldRejectSelfDisable() {
        when(currentUserService.getCurrentUser()).thenReturn(adminPrincipal(2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.disableUser(2L, request)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(userAccountRepository, never()).findById(any());
        verify(securityAuditService, never()).recordAdminUserStatusChange(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnUserDetailWithAssetSummaryAndAudit() {
        UserAccountEntity target = createAccount(2L, "Target User");
        UserIdentityEntity identity = UserIdentityEntity.createLocal(target, "target", "encoded");
        when(currentUserService.getCurrentUser()).thenReturn(adminPrincipal(1L));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userIdentityRepository.findByUserAccountIdAndProvider(2L, "LOCAL")).thenReturn(Optional.of(identity));
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(2L, 3L, 4L);

        AdminUserDetailResponse response = adminUserService.getUserDetail(2L, request);

        assertEquals(2L, response.id());
        assertEquals("target", response.username());
        assertEquals(2L, response.assetSummary().wordSetCount());
        assertEquals(3L, response.assetSummary().studyPlanCount());
        assertEquals(4L, response.assetSummary().noteCount());
        verify(securityAuditService).recordAdminUserDetailView(request, 1L, "admin", 2L);
    }

    @Test
    void shouldResetTargetUserPasswordAndRecordAudit() {
        UserAccountEntity target = createAccount(2L, "Target User");
        UserIdentityEntity identity = UserIdentityEntity.createLocal(target, "target", "encoded");
        when(currentUserService.getCurrentUser()).thenReturn(adminPrincipal(1L));
        when(userIdentityRepository.findByUserAccountIdAndProvider(2L, "LOCAL")).thenReturn(Optional.of(identity));
        when(passwordEncoder.matches("newPassword1", "encoded")).thenReturn(false);
        when(passwordEncoder.encode("newPassword1")).thenReturn("encoded-new");

        AdminUserPasswordResetResponse response = adminUserService.resetUserPassword(
                2L,
                new AdminResetPasswordRequest("newPassword1"),
                request
        );

        assertEquals(2L, response.id());
        assertEquals("encoded-new", identity.getPasswordHash());
        verify(userIdentityRepository).save(identity);
        verify(securityAuditService).recordAdminUserPasswordReset(request, 1L, "admin", 2L);
    }

    @Test
    void shouldRejectSelfPasswordReset() {
        when(currentUserService.getCurrentUser()).thenReturn(adminPrincipal(2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.resetUserPassword(2L, new AdminResetPasswordRequest("newPassword1"), request)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(userIdentityRepository, never()).findByUserAccountIdAndProvider(any(), any());
        verify(securityAuditService, never()).recordAdminUserPasswordReset(any(), any(), any(), any());
    }

    private AppUserPrincipal adminPrincipal(Long userId) {
        return new AppUserPrincipal(userId, "admin", "Admin User", "encoded", "ADMIN", true);
    }

    private UserAccountEntity createAccount(Long id, String displayName) {
        UserAccountEntity account = UserAccountEntity.create(displayName);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
