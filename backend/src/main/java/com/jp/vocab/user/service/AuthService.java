package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AppUserPrincipal;
import com.jp.vocab.shared.auth.AuthProvider;
import com.jp.vocab.shared.config.AuthLoginProperties;
import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LoginRequest;
import com.jp.vocab.user.dto.LogoutResponse;
import com.jp.vocab.user.dto.RegisterRequest;
import com.jp.vocab.user.entity.UserAccountEntity;
import com.jp.vocab.user.entity.UserIdentityEntity;
import com.jp.vocab.user.entity.UserSettingEntity;
import com.jp.vocab.user.repository.UserAccountRepository;
import com.jp.vocab.user.repository.UserIdentityRepository;
import com.jp.vocab.user.repository.UserSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserProfileService userProfileService;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthLoginProperties authLoginProperties;
    private final SecurityAuditService securityAuditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserProfileService userProfileService,
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            UserSettingRepository userSettingRepository,
            PasswordEncoder passwordEncoder,
            AuthLoginProperties authLoginProperties,
            SecurityAuditService securityAuditService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userProfileService = userProfileService;
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.authLoginProperties = authLoginProperties;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public CurrentUserResponse login(LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String username = request.username().trim();
        OffsetDateTime now = OffsetDateTime.now();
        Optional<UserIdentityEntity> localIdentity = userIdentityRepository.findByProviderAndProviderSubject(AuthProvider.LOCAL, username);
        localIdentity.ifPresent(identity -> validateLoginLock(identity, now, servletRequest, username));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            username,
                            request.password()
                    )
            );

            localIdentity.ifPresent(this::clearLoginFailureState);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
            servletRequest.getSession(true);
            CurrentUserResponse response = userProfileService.getCurrentUserProfile();
            securityAuditService.recordLoginSuccess(servletRequest, response.id(), response.username());
            return response;
        } catch (BadCredentialsException ex) {
            localIdentity.ifPresent(identity -> recordLoginFailure(identity, now));
            recordFailedLoginAudit(servletRequest, localIdentity, username, now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        } catch (DisabledException ex) {
            securityAuditService.recordLoginDisabled(servletRequest, localIdentity.orElse(null), username);
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is disabled");
        }
    }

    private void validateLoginLock(
            UserIdentityEntity identity,
            OffsetDateTime now,
            HttpServletRequest servletRequest,
            String username
    ) {
        if (identity.isLoginLocked(now)) {
            securityAuditService.recordLoginLocked(servletRequest, identity, username);
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is temporarily locked");
        }
        if (identity.hasExpiredLoginLock(now)) {
            identity.clearLoginFailureState();
            userIdentityRepository.save(identity);
        }
    }

    private void recordFailedLoginAudit(
            HttpServletRequest servletRequest,
            Optional<UserIdentityEntity> localIdentity,
            String username,
            OffsetDateTime now
    ) {
        if (localIdentity.isPresent() && localIdentity.get().isLoginLocked(now)) {
            securityAuditService.recordLoginLocked(servletRequest, localIdentity.get(), username);
            return;
        }
        securityAuditService.recordLoginFailure(servletRequest, localIdentity.orElse(null), username);
    }

    private void recordLoginFailure(UserIdentityEntity identity, OffsetDateTime failedAt) {
        identity.recordFailedLogin(
                failedAt,
                authLoginProperties.getMaxFailedAttempts(),
                authLoginProperties.getLockDuration()
        );
        userIdentityRepository.save(identity);
    }

    private void clearLoginFailureState(UserIdentityEntity identity) {
        if (identity.hasLoginFailureState()) {
            identity.clearLoginFailureState();
            userIdentityRepository.save(identity);
        }
    }

    @Transactional
    public CurrentUserResponse register(
            RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String username = normalizeUsername(request.username());
        String displayName = normalizeDisplayName(request.displayName());
        validateNewPassword(request.password());

        if (userIdentityRepository.existsByProviderAndProviderSubject(AuthProvider.LOCAL, username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Username already exists: " + username);
        }

        UserAccountEntity account = userAccountRepository.save(UserAccountEntity.create(displayName));
        userIdentityRepository.save(UserIdentityEntity.createLocal(
                account,
                username,
                passwordEncoder.encode(request.password())
        ));
        userSettingRepository.save(UserSettingEntity.create(account.getId(), null));

        return login(new LoginRequest(username, request.password()), servletRequest, servletResponse);
    }

    public LogoutResponse logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = authenticatedUserId(authentication);
        String username = authentication == null ? null : authentication.getName();
        securityAuditService.recordLogout(servletRequest, userId, username);

        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), servletRequest, servletResponse);
        return new LogoutResponse(true);
    }

    private Long authenticatedUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "username must not be blank");
        }
        if (!normalized.matches("[a-z0-9._-]{3,64}")) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "username must be 3-64 chars and contain only lowercase letters, digits, dot, underscore, or hyphen"
            );
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "displayName must not be blank");
        }
        if (normalized.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "displayName must be at most 128 characters");
        }
        return normalized;
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "password must not be blank");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "password must be between 8 and 72 characters");
        }
    }
}
