package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.auth.AppUserPrincipal;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MobileAuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MOBILE_USERNAME_PREFIX = "wechat-miniapp:";

    private final WechatMiniappClient wechatMiniappClient;
    private final MobileSessionService mobileSessionService;
    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserSettingRepository userSettingRepository;
    private final SecurityAuditService securityAuditService;

    public MobileAuthService(
            WechatMiniappClient wechatMiniappClient,
            MobileSessionService mobileSessionService,
            CurrentUserService currentUserService,
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            UserSettingRepository userSettingRepository,
            SecurityAuditService securityAuditService
    ) {
        this.wechatMiniappClient = wechatMiniappClient;
        this.mobileSessionService = mobileSessionService;
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userSettingRepository = userSettingRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public MobileAuthSessionResponse login(WechatMiniappLoginRequest request, HttpServletRequest servletRequest) {
        WechatMiniappSession wechatSession = wechatMiniappClient.exchangeCode(request.code().trim());
        UserAccountEntity account = findOrCreateAccount(wechatSession.openid());

        if (!UserAccountEntity.STATUS_ACTIVE.equals(account.getStatus())) {
            securityAuditService.recordLoginDisabled(servletRequest, null, mobileSessionService.mobileUsername(account.getId()));
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is disabled");
        }

        MobileSessionToken token = mobileSessionService.issueToken(account);
        CurrentUserResponse user = toMobileUserResponse(account);
        securityAuditService.recordLoginSuccess(servletRequest, user.id(), user.username());
        return new MobileAuthSessionResponse(token.token(), token.expiresAt(), user);
    }

    @Transactional
    public CurrentUserResponse getCurrentMobileUser() {
        AppUserPrincipal principal = requireMobilePrincipal();
        Long userId = principal.getUserId();
        UserAccountEntity account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User account not found: " + userId));
        return toMobileUserResponse(account);
    }

    public LogoutResponse logout(HttpServletRequest servletRequest) {
        AppUserPrincipal principal = requireMobilePrincipal();
        Long userId = principal.getUserId();
        String rawToken = resolveBearerToken(servletRequest);
        mobileSessionService.revokeToken(rawToken);
        securityAuditService.recordLogout(servletRequest, userId, mobileSessionService.mobileUsername(userId));
        return new LogoutResponse(true);
    }

    private UserAccountEntity findOrCreateAccount(String openid) {
        return userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.WECHAT_MINIAPP, openid)
                .map(UserIdentityEntity::getUserAccount)
                .orElseGet(() -> createWechatAccount(openid));
    }

    private UserAccountEntity createWechatAccount(String openid) {
        UserAccountEntity account = userAccountRepository.save(UserAccountEntity.create("WeChat Learner", UserRole.USER));
        userIdentityRepository.save(UserIdentityEntity.createExternal(account, AuthProvider.WECHAT_MINIAPP, openid));
        userSettingRepository.save(UserSettingEntity.create(account.getId(), LearningOrder.WORD_FIRST));
        return account;
    }

    private CurrentUserResponse toMobileUserResponse(UserAccountEntity account) {
        UserSettingEntity setting = userSettingRepository.findById(account.getId())
                .orElse(UserSettingEntity.create(account.getId(), LearningOrder.WORD_FIRST));
        return new CurrentUserResponse(
                account.getId(),
                mobileSessionService.mobileUsername(account.getId()),
                account.getDisplayName(),
                setting.getPreferredLearningOrder(),
                List.of(UserRole.USER)
        );
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mobile token is required");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private AppUserPrincipal requireMobilePrincipal() {
        AppUserPrincipal principal = currentUserService.getCurrentUser();
        if (!principal.getUsername().startsWith(MOBILE_USERNAME_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Mobile token is required");
        }
        return principal;
    }
}
