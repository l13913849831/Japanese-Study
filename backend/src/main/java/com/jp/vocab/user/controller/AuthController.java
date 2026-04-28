package com.jp.vocab.user.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LoginRequest;
import com.jp.vocab.user.dto.LogoutResponse;
import com.jp.vocab.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        return ApiResponse.success(authService.login(request, servletRequest, servletResponse));
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        return ApiResponse.success(authService.logout(servletRequest, servletResponse));
    }
}
