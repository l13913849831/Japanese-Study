package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.CurrentUserService;
import com.jp.vocab.shared.auth.LearningOrder;
import com.jp.vocab.shared.auth.UserRole;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LogoutResponse;
import com.jp.vocab.user.dto.MobileAuthSessionResponse;
import com.jp.vocab.user.dto.WechatMiniappLoginRequest;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileAuthServiceTest {

    @Mock
    private WechatMiniappClient wechatMiniappClient;

    @Mock
    private MobileSessionService mobileSessionService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private SecurityAuditService securityAuditService;

    private MobileAuthService mobileAuthService;

    @BeforeEach
    void setUp() {
        mobileAuthService = new MobileAuthService(
                wechatMiniappClient,
                mobileSessionService,
                currentUserService,
                userAccountRepository,
                userIdentityRepository,
                userSettingRepository,
                securityAuditService
        );
        lenient().when(mobileSessionService.mobileUsername(anyLong()))
                .thenAnswer(invocation -> "wechat-miniapp:" + invocation.getArgument(0));
    }

    @Test
    void shouldCreateAccountAndIssueTokenForNewOpenid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-10T12:00:00+09:00");

        when(wechatMiniappClient.exchangeCode("code-1")).thenReturn(new WechatMiniappSession("openid-1"));
        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.WECHAT_MINIAPP, "openid-1"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 7L);
            return account;
        });
        when(mobileSessionService.issueToken(any(UserAccountEntity.class)))
                .thenReturn(new MobileSessionToken("raw-token", expiresAt));

        MobileAuthSessionResponse response = mobileAuthService.login(new WechatMiniappLoginRequest("  code-1  "), request);

        assertEquals("raw-token", response.token());
        assertEquals(expiresAt, response.expiresAt());
        assertEquals(7L, response.user().id());
        assertEquals("wechat-miniapp:7", response.user().username());
        assertEquals("WeChat Learner", response.user().displayName());
        assertEquals("WORD_FIRST", response.user().preferredLearningOrder());
        assertEquals(List.of(UserRole.USER), response.user().roles());

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertEquals("WeChat Learner", accountCaptor.getValue().getDisplayName());
        assertEquals(UserRole.USER, accountCaptor.getValue().getRole());

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertEquals(AuthProvider.WECHAT_MINIAPP, identityCaptor.getValue().getProvider());
        assertEquals("openid-1", identityCaptor.getValue().getProviderSubject());

        ArgumentCaptor<UserSettingEntity> settingCaptor = ArgumentCaptor.forClass(UserSettingEntity.class);
        verify(userSettingRepository).save(settingCaptor.capture());
        assertEquals(7L, settingCaptor.getValue().getUserId());
        assertEquals(LearningOrder.WORD_FIRST, settingCaptor.getValue().getPreferredLearningOrder());

        verify(mobileSessionService).issueToken(accountCaptor.getValue());
        verify(securityAuditService).recordLoginSuccess(request, 7L, "wechat-miniapp:7");
    }

    @Test
    void shouldReuseExistingAccountForKnownOpenid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-10T13:00:00+09:00");
        UserAccountEntity account = account(9L, "Existing Learner", UserAccountEntity.STATUS_ACTIVE);
        UserIdentityEntity identity = UserIdentityEntity.createExternal(account, AuthProvider.WECHAT_MINIAPP, "openid-2");
        UserSettingEntity setting = UserSettingEntity.create(9L, LearningOrder.NOTE_FIRST);

        when(wechatMiniappClient.exchangeCode("code-2")).thenReturn(new WechatMiniappSession("openid-2"));
        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.WECHAT_MINIAPP, "openid-2"))
                .thenReturn(Optional.of(identity));
        when(userSettingRepository.findById(9L)).thenReturn(Optional.of(setting));
        when(mobileSessionService.issueToken(account)).thenReturn(new MobileSessionToken("existing-token", expiresAt));

        MobileAuthSessionResponse response = mobileAuthService.login(new WechatMiniappLoginRequest("code-2"), request);

        assertEquals("existing-token", response.token());
        assertEquals("NOTE_FIRST", response.user().preferredLearningOrder());
        assertEquals("wechat-miniapp:9", response.user().username());

        verify(userAccountRepository, never()).save(any());
        verify(userIdentityRepository, never()).save(any());
        verify(userSettingRepository, never()).save(any());
        verify(securityAuditService).recordLoginSuccess(request, 9L, "wechat-miniapp:9");
    }

    @Test
    void shouldRejectDisabledAccountOnWechatLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserAccountEntity account = account(11L, "Disabled Learner", UserAccountEntity.STATUS_DISABLED);
        UserIdentityEntity identity = UserIdentityEntity.createExternal(account, AuthProvider.WECHAT_MINIAPP, "openid-3");

        when(wechatMiniappClient.exchangeCode("code-3")).thenReturn(new WechatMiniappSession("openid-3"));
        when(userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.WECHAT_MINIAPP, "openid-3"))
                .thenReturn(Optional.of(identity));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mobileAuthService.login(new WechatMiniappLoginRequest("code-3"), request)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(mobileSessionService, never()).issueToken(any());
        verify(securityAuditService).recordLoginDisabled(eq(request), isNull(), eq("wechat-miniapp:11"));
    }

    @Test
    void shouldReturnCurrentMobileUserForMobilePrincipal() {
        UserAccountEntity account = account(13L, "Mobile Learner", UserAccountEntity.STATUS_ACTIVE);
        UserSettingEntity setting = UserSettingEntity.create(13L, LearningOrder.NOTE_FIRST);

        when(currentUserService.getCurrentUser()).thenReturn(mobilePrincipal(13L));
        when(userAccountRepository.findById(13L)).thenReturn(Optional.of(account));
        when(userSettingRepository.findById(13L)).thenReturn(Optional.of(setting));

        CurrentUserResponse response = mobileAuthService.getCurrentMobileUser();

        assertEquals(13L, response.id());
        assertEquals("wechat-miniapp:13", response.username());
        assertEquals("Mobile Learner", response.displayName());
        assertEquals("NOTE_FIRST", response.preferredLearningOrder());
        assertEquals(List.of(UserRole.USER), response.roles());
    }

    @Test
    void shouldRejectWebPrincipalWhenFetchingCurrentMobileUser() {
        when(currentUserService.getCurrentUser()).thenReturn(new AppUserPrincipal(15L, "demo", "Demo User", "encoded", UserRole.USER, true));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mobileAuthService.getCurrentMobileUser()
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(userAccountRepository, userSettingRepository);
    }

    @Test
    void shouldRevokeMobileTokenOnLogout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer raw-token");

        when(currentUserService.getCurrentUser()).thenReturn(mobilePrincipal(17L));

        LogoutResponse response = mobileAuthService.logout(request);

        assertEquals(true, response.loggedOut());
        verify(mobileSessionService).revokeToken("raw-token");
        verify(securityAuditService).recordLogout(request, 17L, "wechat-miniapp:17");
    }

    private AppUserPrincipal mobilePrincipal(Long userId) {
        return new AppUserPrincipal(userId, "wechat-miniapp:" + userId, "Mobile Learner", "token", UserRole.USER, true);
    }

    private UserAccountEntity account(Long id, String displayName, String status) {
        UserAccountEntity account = UserAccountEntity.create(displayName, UserRole.USER);
        ReflectionTestUtils.setField(account, "id", id);
        if (UserAccountEntity.STATUS_DISABLED.equals(status)) {
            account.disable();
        }
        return account;
    }
}
