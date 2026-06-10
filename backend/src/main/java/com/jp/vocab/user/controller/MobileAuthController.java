package com.jp.vocab.user.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.LogoutResponse;
import com.jp.vocab.user.dto.MobileAuthSessionResponse;
import com.jp.vocab.user.dto.WechatMiniappLoginRequest;
import com.jp.vocab.user.service.MobileAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    public MobileAuthController(MobileAuthService mobileAuthService) {
        this.mobileAuthService = mobileAuthService;
    }

    @PostMapping("/auth/wechat-login")
    public ApiResponse<MobileAuthSessionResponse> login(
            @Valid @RequestBody WechatMiniappLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(mobileAuthService.login(request, servletRequest));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<LogoutResponse> logout(HttpServletRequest servletRequest) {
        return ApiResponse.success(mobileAuthService.logout(servletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentMobileUser() {
        return ApiResponse.success(mobileAuthService.getCurrentMobileUser());
    }
}
