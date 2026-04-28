package com.jp.vocab.user.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.user.dto.ChangePasswordRequest;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.PasswordChangeResponse;
import com.jp.vocab.user.dto.UpdateUserProfileRequest;
import com.jp.vocab.user.dto.UpdateUserSettingsRequest;
import com.jp.vocab.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<CurrentUserResponse> getCurrentUserProfile() {
        return ApiResponse.success(userProfileService.getCurrentUserProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<CurrentUserResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.success(userProfileService.updateProfile(request));
    }

    @PutMapping("/settings")
    public ApiResponse<CurrentUserResponse> updateSettings(
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ApiResponse.success(userProfileService.updateSettings(request));
    }

    @PutMapping("/password")
    public ApiResponse<PasswordChangeResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ApiResponse.success(userProfileService.changePassword(request));
    }
}
