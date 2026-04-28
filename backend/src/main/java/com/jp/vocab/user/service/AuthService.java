package com.jp.vocab.user.service;

import com.jp.vocab.shared.auth.AuthProvider;
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

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserProfileService userProfileService;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserProfileService userProfileService,
            UserAccountRepository userAccountRepository,
            UserIdentityRepository userIdentityRepository,
            UserSettingRepository userSettingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userProfileService = userProfileService;
        this.userAccountRepository = userAccountRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userSettingRepository = userSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse login(LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(),
                            request.password()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
            servletRequest.getSession(true);
            return userProfileService.getCurrentUserProfile();
        } catch (BadCredentialsException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        } catch (DisabledException ex) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User account is disabled");
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
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), servletRequest, servletResponse);
        return new LogoutResponse(true);
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
