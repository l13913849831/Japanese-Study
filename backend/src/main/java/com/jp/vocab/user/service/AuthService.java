package com.jp.vocab.user.service;

import com.jp.vocab.shared.exception.BusinessException;
import com.jp.vocab.shared.exception.ErrorCode;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LoginRequest;
import com.jp.vocab.user.dto.LogoutResponse;
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
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserProfileService userProfileService;

    public AuthService(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserProfileService userProfileService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userProfileService = userProfileService;
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

    public LogoutResponse logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), servletRequest, servletResponse);
        return new LogoutResponse(true);
    }
}
