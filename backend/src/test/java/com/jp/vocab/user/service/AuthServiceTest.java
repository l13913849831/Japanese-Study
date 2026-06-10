package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.config.AuthLoginProperties;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LoginRequest;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityAuditService securityAuditService;

    private AuthLoginProperties authLoginProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authLoginProperties = new AuthLoginProperties();
        authLoginProperties.setMaxFailedAttempts(3);
        authLoginProperties.setLockDuration(Duration.ofMinutes(15));
        authService = new AuthService(
                authenticationManager,
                securityContextRepository,
                userProfileService,
                userAccountRepository,
                userIdentityRepository,
                userSettingRepository,
                passwordEncoder,
                authLoginProperties,
                securityAuditService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecordFailedLoginAndLockIdentityAtThreshold() {
        UserIdentityEntity identity = createIdentity();
        identity.recordFailedLogin(OffsetDateTime.now().minusMinutes(10), 5, Duration.ofMinutes(15));
        identity.recordFailedLogin(OffsetDateTime.now().minusMinutes(5), 5, Duration.ofMinutes(15));

        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.LOCAL, "demo"))
                .thenReturn(Optional.of(identity));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(
                        new LoginRequest("demo", "wrong-password"),
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse()
                )
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertEquals(3, identityCaptor.getValue().getFailedLoginCount());
        assertNotNull(identityCaptor.getValue().getLastFailedLoginAt());
        assertNotNull(identityCaptor.getValue().getLockedUntil());
        verify(securityAuditService).recordLoginLocked(any(), any(), any());
    }

    @Test
    void shouldRejectTemporarilyLockedIdentityBeforeAuthentication() {
        UserIdentityEntity identity = createIdentity();
        identity.recordFailedLogin(OffsetDateTime.now(), 1, Duration.ofMinutes(15));

        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.LOCAL, "demo"))
                .thenReturn(Optional.of(identity));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(
                        new LoginRequest("demo", "secret"),
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse()
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("User account is temporarily locked", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
        verify(userIdentityRepository, never()).save(any());
        verify(securityAuditService).recordLoginLocked(any(), any(), any());
    }

    @Test
    void shouldClearFailureStateAfterSuccessfulLogin() {
        UserIdentityEntity identity = createIdentity();
        identity.recordFailedLogin(OffsetDateTime.now().minusMinutes(10), 5, Duration.ofMinutes(15));

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AppUserPrincipal(1L, "demo", "Demo User", "encoded", "USER", true),
                "secret",
                List.of()
        );
        CurrentUserResponse expectedResponse = new CurrentUserResponse(1L, "demo", "Demo User", "WORD_FIRST", List.of("USER"));

        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.LOCAL, "demo"))
                .thenReturn(Optional.of(identity));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userProfileService.getCurrentUserProfile()).thenReturn(expectedResponse);

        CurrentUserResponse response = authService.login(
                new LoginRequest("demo", "secret"),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()
        );

        assertEquals(expectedResponse, response);

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertEquals(0, identityCaptor.getValue().getFailedLoginCount());
        assertNull(identityCaptor.getValue().getLastFailedLoginAt());
        assertNull(identityCaptor.getValue().getLockedUntil());
        verify(securityAuditService).recordLoginSuccess(any(), any(), any());
    }

    private UserIdentityEntity createIdentity() {
        UserAccountEntity account = UserAccountEntity.create("Demo User");
        ReflectionTestUtils.setField(account, "id", 1L);
        return UserIdentityEntity.createLocal(account, "demo", "encoded");
    }
}
